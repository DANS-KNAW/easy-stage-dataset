/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.sql.SQLException

import com.yourmediashelf.fedora.client.FedoraClientException
import nl.knaw.dans.easy.stage.lib.FOXML.{ getDirFOXML, getFileFOXML }
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.easy.stage.lib._
import nl.knaw.dans.easy.stage._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }

object EasyStageFileItem extends DebugEnhancedLogging {

  def main(args: Array[String]) {
    val props = if (args(0) != "--help") {
      log.debug(s"app.home = ${System.getProperty("app.home")}")
      val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
      //props.save(System.out)
      Fedora.setFedoraConnectionSettings(props.getString("fcrepo.url"), props.getString("fcrepo.user"), props.getString("fcrepo.password"))
      props
    } else null // ScallopConf will exit so just satisfy the compiler
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
    }.recover { case t: Throwable => log.error(s"Staging FAIL of $conf with repo url ${props.getString("fcrepo.url")}", t) }
  }

  def getSettingsRows(conf: FileItemConf): Try[Seq[FileItemSettings]] =
    if (conf.datasetId.isDefined)
      Success(Seq(FileItemSettings(conf)))
    else {
      val trailArgs = Seq(conf.sdoSetDir.apply().toString)
      CSV(conf.csvFile.apply(), conf.longOptionNames).map {
        case (csv, warning) =>
          warning.foreach(log.warn)
          val rows = csv.getRows
          if (rows.isEmpty) log.warn(s"Empty CSV file")
          rows.map(options => {
            log.info("Options: "+options.mkString(" "))
            FileItemSettings(new FileItemConf(options ++ trailArgs))
          })
      }
    }

  def run(implicit s: FileItemSettings): Try[Unit] = {
    trace(s)
    for {
      datasetId        <- getValidDatasetId(s)
      sdoSetDir        <- mkdirSafe(s.sdoSetDir)
      datasetSdoSetDir <- mkdirSafe(new File(sdoSetDir, datasetId.replace(":", "_")))
      existingAncestor <- s.easyFilesAndFolders.getExistingAncestor(s.pathInDataset.get, datasetId)
      _                = createFolderSdos(existingAncestor, s.pathInDataset.get, datasetSdoSetDir)
      _                <- createFileSdoForExistingDataset(datasetSdoSetDir, existingAncestor)
    } yield ()
  }

  def getItemsToStage(pathElements: Seq[String],
                      datasetSdoSet: File,
                      existingFolderId: String
                     ): Seq[(File, String, RelationObject)] = {
    trace(pathElements, datasetSdoSet, existingFolderId)
    getPaths(pathElements)
    .foldLeft(Seq[(File, String, RelationObject)]())((items, path) => {
      items match {
        case s@Seq() =>
          val sdoDir = new File(datasetSdoSet, toSdoName(path))
          log.debug(s"$sdoDir $path")
          s :+ (sdoDir, path, "object" -> s"info:fedora/$existingFolderId")
        case seq =>
          val parentFolderSdoName = seq.last match { case (sdo, _,  _) => sdo.getName}
          val sdoDir = new File(datasetSdoSet, toSdoName(path))
          log.debug(s"$sdoDir $path")
          seq :+ (sdoDir, path, "objectSDO" -> parentFolderSdoName)
      }
    })
  }

  private def getPaths(path: Seq[String]): Seq[String] = {
    trace(path)
    if(path.isEmpty) Seq()
    else path.tail.scanLeft(path.head)((acc, next) => s"$acc/$next")
  }

  private def createFolderSdos(existingAncestor: ExistingAncestor,
                               file: File,
                               datasetSdoSetDir: File
                              )(implicit s: FileItemSettings) = {
    val (existingPath, _) = existingAncestor

    @tailrec
    def createParent(child: File): Unit = {
      if (child != null) {
        val parent = child.getParentFile
        if (parent != null && parent.toString != existingPath) {
          val sdoDir = new File(datasetSdoSetDir, toSdoName(parent.toString))
          createFolderSdo(sdoDir, parent.getName, "objectSDO" -> toSdoName(parent.toString))
          createParent(child.getParentFile)
        }
      }
    }
    createParent(file)
  }

  private def createFileSdoForExistingDataset(datasetSdoSetDir: File,
                                              existingAncestor: ExistingAncestor
                                             )(implicit s: FileItemSettings): Try[Unit] = {
    val parentPath = Option(s.pathInDataset.get.getParent).getOrElse("")
    val ancestor = existingAncestor match {
      case ((`parentPath`, fedoraId)) =>
        fedoraId
      case _ =>
        toSdoName(parentPath)
    }
    val sdoDir = new File(datasetSdoSetDir, toSdoName(s.pathInDataset.get.toString))
    val ancestorType = if (ancestor.startsWith("info:fedora")) "object"else "objectSDO"
    createFileSdo(sdoDir, ancestorType -> ancestor)
  }

  def createFileSdo(sdoDir: File, parentRelation: RelationObject)(implicit s: FileItemSettings): Try[Unit] = {
    trace(sdoDir, parentRelation)
    require(s.datastreamLocation.isDefined != s.file.isDefined, s"Exactly one of datastreamLocation and file must be defined (datastreamLocation = ${s.datastreamLocation}, file = ${s.file})")
    log.debug(s"Creating file SDO: ${s.pathInDataset.getOrElse("<no path in dataset?>")}")
    sdoDir.mkdir()
    for {
      mime         <- Try { s.format.get }
      cfgContent   <- Try { JSON.createFileCfg(mime, parentRelation, s.subordinate) }
      _            <- writeJsonCfg(sdoDir, cfgContent)
      title        <- Try {s.title.getOrElse(s.pathInDataset.get.getName)}
      foxmlContent  = getFileFOXML(title, s.ownerId.get, mime)
      _            <- writeFoxml(sdoDir, foxmlContent)
      fmd          <- EasyFileMetadata(s)
      _            <- writeFileMetadata(sdoDir, fmd)
      _            <- s.file.flatMap(_ => s.file.map(f =>
        copyFile(sdoDir, f)
        )).getOrElse(Success(Unit))
    } yield ()
  }

  def createFolderSdo(sdoDir: File, path: String, parent: RelationObject)(implicit s: FileItemSettings): Try[Unit] = {
    trace(sdoDir, path, parent)
    sdoDir.mkdir()
    for {
      _ <- writeJsonCfg(sdoDir,JSON.createDirCfg(parent, s.subordinate))
      _ <- writeFoxml(sdoDir, getDirFOXML(path, s.ownerId.get))
      _ <- writeItemContainerMetadata(sdoDir,EasyItemContainerMd(path))
    } yield ()
  }

  private def getValidDatasetId(s: FileItemSettings): Try[String] =
    if (s.datasetId.isEmpty)
      Failure(new Exception(s"no datasetId provided"))
    else if (s.fedora.findObjects(s"pid~${s.datasetId.get}").isEmpty)
      Failure(new Exception(s"${s.datasetId.get} does not exist in repository"))
    else
      Success(s.datasetId.get)

  def toSdoName(path: String): String = {
    trace(path)
    path.replaceAll("[/.]", "_").replaceAll("^_", "")
  }
}
