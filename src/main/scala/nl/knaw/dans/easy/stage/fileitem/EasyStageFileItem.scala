/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL
import java.sql.SQLException

import com.yourmediashelf.fedora.client.FedoraClientException
import nl.knaw.dans.easy.stage.lib.FOXML.{getDirFOXML, getFileFOXML}
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.easy.stage.lib._
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyStageFileItem {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
    Fedora.setFedoraConnectionSettings(props.getString("fcrepo.url"), props.getString("fcrepo.user"), props.getString("fcrepo.password"))
    val conf = new FileItemConf(args)
    getSettingsRows(conf).map {
      _.foreach { settings =>
        run(settings)
          .map(_ => log.info(s"Staging SUCCESS of $settings"))
          .recover { case t: Throwable =>
            log.error(s"Staging FAIL of $settings", t)
            if (t.isInstanceOf[SQLException] || t.isInstanceOf[FedoraClientException]) return
          }
      }
    }.recover { case t: Throwable => log.error(s"Staging FAIL of $conf", t) }
  }

  private def getSettingsRows(conf: FileItemConf): Try[Seq[FileItemSettings]] =
    if (conf.datasetId.isDefined)
      Success(Seq(FileItemSettings(conf)))
    else if (conf.csvFile.isEmpty)
      Failure(new Exception("neither datasetId (option -i) nor CSV file (optional trail argument) specified"))
    else {
      val trailArgs = Seq(conf.sdoSetDir.apply().toString)
      CSV(conf.csvFile.apply(), conf.longOptionNames).map {
        case (csv, warning) =>
          warning.map(msg => log.warn(msg))
          val rows = csv.getRows
          if (rows.isEmpty) log.warn(s"Empty CSV file")
          rows.map(options => FileItemSettings(options ++ trailArgs))
      }
    }

  def run(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"executing: $s")
    for {
      datasetId        <- getValidDatasetId(s)
      sdoSetDir        <- mkdirSafe(s.sdoSetDir)
      datasetSdoSetDir <- mkdirSafe(new File(sdoSetDir, datasetId.replace(":", "_")))
      (parentId, parentPath, newElements)  <- getPathElements()
      items            <- Try { getItemsToStage(newElements, datasetSdoSetDir, parentId) }
      _                = log.debug(s"Items to stage: $items")
      _                = items.init.foreach { case (sdo, path, parentRelation) => createFolderSdo(sdo, fullPath(parentPath, path).toString, parentRelation) }
      _                <- items.last match {case (sdo, path, parentRelation) => createFileSdo(sdo, fullPath(parentPath, path).toString, parentRelation) }
    } yield ()
  }

  def fullPath(parentPath: String, path: String): File =
    if (parentPath.isEmpty) new File(path) // prevent a leading slash
    else new File(parentPath, path)

  def getPathElements()(implicit s: FileItemSettings): Try[(String, String, Seq[String])] = {
    val file = s.pathInDataset.get
    EasyFilesAndFolders.getExistingAncestor(file, s.datasetId.get)
      .map { case (parentPath, parentId) =>
        log.debug(s"Parent in repository: $parentId $parentPath")
        val newItems = file.toString.replaceFirst(s"^$parentPath/", "").split("/")
        (parentId, parentPath, newItems.toSeq)
      }
  }

  def getItemsToStage(pathElements: Seq[String], datasetSdoSet: File, existingFolderId: String): Seq[(File, String, (String, String))] = {
    getPaths(pathElements)
    .foldLeft(Seq[(File, String, (String, String))]())((items, path) => {
      items match {
        case s@Seq() => s :+ (new File(datasetSdoSet, toSdoName(path)), path, "object" -> s"info:fedora/$existingFolderId")
        case seq =>
          val parentFolderSdoName = seq.last match { case (sdo, _,  _) => sdo.getName}
          seq :+ (new File(datasetSdoSet, toSdoName(path)), path, "objectSDO" -> parentFolderSdoName)
      }
    })
  }

  def getPaths(path: Seq[String]): Seq[String] =
    if(path.isEmpty) Seq()
    else path.tail.scanLeft(path.head)((acc, next) => s"$acc/$next")


  def createFileSdo(sdoDir: File, path: String, parent: (String,String))(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating file SDO: $path")
    sdoDir.mkdir()
    for {
      mime <- Try{s.format.get}
      _ <- writeJsonCfg(sdoDir, JSON.createFileCfg(s.dsLocation.getOrElse(s.unsetUrl), mime, parent, s.subordinate))
      _ <- writeFoxml(sdoDir, getFileFOXML(s.pathInDataset.get.getName, s.ownerId, mime))
      fmd <- EasyFileMetadata(s)
      _ <- writeFileMetadata(sdoDir, fmd)
    } yield ()
  }

  def createFolderSdo(sdoDir: File, path: String, parent: (String,String))(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating folder SDO: $path")
    sdoDir.mkdir()
    for {
      _ <- writeJsonCfg(sdoDir,JSON.createDirCfg(parent, s.subordinate))
      _ <- writeFoxml(sdoDir, getDirFOXML(path, s.ownerId))
      _ <- writeItemContainerMetadata(sdoDir,EasyItemContainerMd(path))
    } yield ()
  }

  private def getValidDatasetId(s: FileItemSettings): Try[String] =
    if (s.datasetId.isEmpty)
      Failure(new Exception(s"no datasetId provided"))
    else if (Fedora.findObjects(s"pid~${s.datasetId.get}").isEmpty)
      Failure(new Exception(s"${s.datasetId.get} does not exist in repository"))
    else
      Success(s.datasetId.get)

  def toSdoName(path: String): String =
    path.replaceAll("[/.]", "_").replaceAll("^_", "")
}
