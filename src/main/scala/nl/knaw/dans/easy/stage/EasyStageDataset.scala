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
package nl.knaw.dans.easy.stage

import java.io.{File, FileNotFoundException}
import java.net.{URI, URL, URLEncoder}
import java.nio.file.{Path, Paths}

import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.easy.stage.dataset.AMD.AdministrativeMetadata
import nl.knaw.dans.easy.stage.dataset.Util._
import nl.knaw.dans.easy.stage.dataset._
import nl.knaw.dans.easy.stage.fileitem.{EasyStageFileItem, FileAccessRights, FileItemSettings}
import nl.knaw.dans.easy.stage.lib.Constants._
import nl.knaw.dans.easy.stage.lib.FOXML._
import nl.knaw.dans.easy.stage.lib.JSON
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.pf.language.emd.EasyMetadata
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils.readFileToString
import org.slf4j.LoggerFactory
import nl.knaw.dans.lib.error._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

object EasyStageDataset {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
    implicit val s = Settings(new Conf(args),props)
    run match {
      case Success(_) => log.info("Staging SUCCESS")
      case Failure(t) => log.error("Staging FAIL", t)
    }
  }

  def run(implicit s: Settings): Try[(EasyMetadata, AdministrativeMetadata)] = {

    def createDatasetSdo(): Try[(EasyMetadata, AdministrativeMetadata)] = {
      log.info("Creating dataset SDO")
      for {
        sdoDir <- mkdirSafe(new File(s.sdoSetDir, DATASET_SDO))
        amdContent = AMD(s.ownerId, s.submissionTimestamp, s.doi.isEmpty)
        emdContent <- EMD.create(sdoDir)
        foxmlContent = getDatasetFOXML(s.ownerId, emdContent)
        mimeType <- AdditionalLicense.createOptionally(sdoDir)
        audiences <- readAudiences()
        jsonCfgContent <- JSON.createDatasetCfg(mimeType, audiences)
        _ <- writeAMD(sdoDir, amdContent.toString())
        _ <- writeFoxml(sdoDir, foxmlContent)
        _ <- writePrsql(sdoDir, PRSQL.create())
        _ <- writeJsonCfg(sdoDir, jsonCfgContent)
      } yield (emdContent, amdContent) // easy-ingest-flow hands these over to easy-ingest
    }

    def getDataDir = Try {
      s.bagitDir.listFiles.find(_.getName == "data")
        .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))
    }

    log.debug(s"Settings = $s")
    for {
      dataDir <- getDataDir
      _ <- mkdirSafe(s.sdoSetDir)
      (emdContent, amdContent) <- createDatasetSdo()
      _ = log.info("Creating file and folder SDOs")
      _ <- createFileAndFolderSdos(dataDir, DATASET_SDO, emdContent.getEmdRights.getAccessCategory)
    } yield (emdContent, amdContent)
  }

  def createFileAndFolderSdos(dir: File, parentSDO: String, rights: AccessCategory)(implicit s: Settings): Try[Unit] = {
    val maybeSha1Map: Try[Map[String, String]] = Try {
      val sha1File = "manifest-sha1.txt"
      readFileToString(new File(s.bagitDir, sha1File))
        .lines.filter(_.nonEmpty)
        .map(_.split("\\h+")) // split into tokens on sequences of horizontal white space characters
        .map {
          case Array(sha1, filePath) if !sha1.matches("[a-fA-F0-9]") => filePath -> sha1
          case array => throw new IllegalArgumentException(s"Invalid line in $sha1File: ${array.mkString(" ")}")
        }.toMap
    }.recoverWith { case e: FileNotFoundException => Success(Map[String, String]()) }

    def createFileAndFolderSdos(dir: File, parentSDO: String): Try[Unit] = {
      log.debug(s"Creating file and folder SDOs for directory: $dir")
      def visit(child: File): Try[Unit] =
        if (child.isFile)
          createFileSdo(child, parentSDO)
        else if (child.isDirectory)
          createFolderSdo(child, parentSDO).flatMap(_ => createFileAndFolderSdos(child, getSDODir(child).getName))
        else
          Failure(new RuntimeException(s"Unknown object encountered while traversing ${dir.getName}: ${child.getName}"))
      Try { dir.listFiles().toList }.flatMap(_.map(visit).collectResults.map(_ => ()))
    }

    def createFileSdo(file: File, parentSDO: String): Try[Unit] = {
      log.debug(s"Creating file SDO for $file")
      val datasetRelativePath = getDatasetRelativePath(file)
      val urlEncodedDatasetRelativePath = Paths.get("", datasetRelativePath.asScala.map {case p => URLEncoder.encode(p.toString, "UTF-8") }.toArray :_*)
      for {
        sdoDir <- mkdirSafe(getSDODir(file))
        bagRelativePath = s.bagitDir.toPath.relativize(file.toPath).toString
        mime <- readMimeType(bagRelativePath)
        title <- readTitle(bagRelativePath)
        fis = FileItemSettings(
          sdoSetDir = s.sdoSetDir,
          file = if (s.stageFileDataAsRedirectDatastreams) None else Some(file),
          datastreamLocation = if (s.stageFileDataAsRedirectDatastreams) s.fileDataRedirectBaseUrl.map(baseUrl => new URL(baseUrl, urlEncodedDatasetRelativePath.toString))
                               else None,
          ownerId = s.ownerId,
          pathInDataset = new File(datasetRelativePath.toString),
          size = Some(file.length),
          format = Some(mime),
          sha1 = maybeSha1Map.get.get(bagRelativePath), // first get is checked in advance
          title = title,
          accessibleTo = FileAccessRights.accessibleTo(rights),
          visibleTo = FileAccessRights.visibleTo(rights)
        )
        _ <- EasyStageFileItem.createFileSdo(sdoDir, "objectSDO" -> parentSDO)(fis)
      } yield ()
    }

    def createFolderSdo(folder: File, parentSDO: String): Try[Unit] = {
      log.debug(s"Creating folder SDO for $folder")
      val relativePath= getDatasetRelativePath(folder).toString
      for {
        sdoDir <- mkdirSafe(getSDODir(folder))
        fis     = FileItemSettings(s.sdoSetDir, s.ownerId, getDatasetRelativePath(folder).toFile)
        _      <- EasyStageFileItem.createFolderSdo(sdoDir, relativePath, "objectSDO" -> parentSDO)(fis)
      } yield ()
    }

    def getSDODir(fileOrDir: File): File = {
      val sdoName = getDatasetRelativePath(fileOrDir).toString.replace("/", "_").replace(".", "_") match {
        case name if name.startsWith("_") => name.tail
        case name => name
      }
      new File(s.sdoSetDir.getPath, sdoName)
    }

    def getDatasetRelativePath(item: File): Path =
      new File(s.bagitDir, "data").toPath.relativize(item.toPath)

    for {
      _ <- maybeSha1Map
      _ <- createFileAndFolderSdos(dir, parentSDO)
    } yield Unit
  }
}
