/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy.stage

import java.io.{ File, FileNotFoundException }
import java.nio.file.Path

import gov.loc.repository.bagit.reader.BagReader
import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.easy.stage.dataset.AMD.AdministrativeMetadata
import nl.knaw.dans.easy.stage.dataset.Util._
import nl.knaw.dans.easy.stage.dataset._
import nl.knaw.dans.easy.stage.fileitem.{ EasyStageFileItem, FileAccessRights, FileItemSettings }
import nl.knaw.dans.easy.stage.lib.Constants._
import nl.knaw.dans.easy.stage.lib.FOXML._
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.easy.stage.lib.{ JSON, SdoRelationObject }
import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.pf.language.emd.EasyMetadata
import org.apache.commons.io.FileUtils.readFileToString

import scala.collection.JavaConverters._
import scala.util.{ Failure, Success, Try }
import scala.xml.NodeSeq

object EasyStageDataset extends DebugEnhancedLogging {
  private val bagReader = new BagReader()

  // TODO: candidate for a possible dans-bagit-lib
  /**
   * Checks that all paths in `files` are part of the payload of the bag in `bagDir`. This means that they must be in at least one payload manifest.
   *
   * @param files  the files to check
   * @param bagDir the directory containing the bag
   * @return Success if all files were part, otherwise Failure
   */
  def checkFilesInBag(files: Set[Path], bagDir: Path): Try[Unit] = {
    for {
      bag <- Try { bagReader.read(bagDir) }
      result <- {
        val filesInBag = for {
          manifest <- bag.getPayLoadManifests.asScala
          path <- manifest.getFileToChecksumMap.keySet().asScala
        } yield bagDir.relativize(path)

        files diff filesInBag match {
          case fs if fs.isEmpty => Success(())
          case fs => Failure(RejectedDepositException(s"The fileUris map must reference a subset of all files in the bag. Not found in bag: $fs"))
        }
      }
    } yield result
  }

  def checkValidState(state: String): Try[Unit] = {
    if (Seq("DRAFT", "SUBMITTED", "PUBLISHED").contains(state)) Success(())
    else Failure(new IllegalArgumentException(s"Not a valid state: $state"))
  }

  def run(implicit s: Settings): Try[(EasyMetadata, AdministrativeMetadata)] = {
    debug(s"Settings = $s")
    val result = for {
      _ <- checkValidState(s.state)
      _ <- checkFilesInBag(s.fileUris.keySet, s.bagitDir.toPath)
      dataDir <- getDataDir
      _ <- mkdirSafe(s.sdoSetDir)
      (emdContent, amdContent) <- createDatasetSdo()
      _ = logger.info("Creating file and folder SDOs")
      _ <- createFileAndFolderSdos(dataDir, DATASET_SDO, emdContent.getEmdRights.getAccessCategory)
    } yield (emdContent, amdContent)

    result
      .doIfSuccess(_ => logger.info("dataset staged"))
      .doIfFailure { case e => logger.error(s"staging failed: ${ e.getMessage }", e) }
  }

  private def createDatasetSdo()(implicit s: Settings): Try[(EasyMetadata, AdministrativeMetadata)] = {
    logger.info("Creating dataset SDO")
    for {
      sdoDir <- mkdirSafe(new File(s.sdoSetDir, DATASET_SDO))
      remarks = DepositorInfo(s.bagitDir.toPath.resolve("metadata/depositor-info"))
      amdContent = AMD(s.ownerId, s.submissionTimestamp, s.state, remarks)
      emdContent <- EMD.create(sdoDir, remarks.acceptedLicense)
      foxmlContent = getDatasetFOXML(s.ownerId, emdContent)
      additionalLicenseFilenameAndMimeType <- AdditionalLicense.createOptionally(sdoDir)
      audiences <- readAudiences()
      jsonCfgContent <- JSON.createDatasetCfg(additionalLicenseFilenameAndMimeType, audiences)
      _ <- writeAMD(sdoDir, amdContent.toString())
      _ <- writeFoxml(sdoDir, foxmlContent)
      _ <- writePrsql(sdoDir, PRSQL.create())
      _ <- writeJsonCfg(sdoDir, jsonCfgContent)
    } yield (emdContent, amdContent) // easy-ingest-flow hands these over to easy-ingest
  }

  private def getDataDir(implicit s: Settings) = {
    s.bagitDir.listFiles
      .collectFirst { case file if file.getName == "data" => Success(file) }
      .getOrElse(Failure(new RuntimeException("Bag doesn't contain data directory.")))
  }

  def createFileAndFolderSdos(dir: File, parentSDO: String, datasetRights: AccessCategory)(implicit s: Settings): Try[Unit] = {
    val maybeSha1Map: Try[Map[String, String]] = Try {
      val sha1File = "manifest-sha1.txt"
      readFileToString(new File(s.bagitDir, sha1File), "UTF-8")
        .lines.filter(_.nonEmpty)
        .map(_.split("\\h+", 2)) // split into tokens on sequences of horizontal white space characters
        .map {
        case Array(sha1, filePath) if !sha1.matches("[a-fA-F0-9]") => filePath -> sha1
        case array => throw new IllegalArgumentException(s"Invalid line in $sha1File: ${ array.mkString(" ") }")
      }
        .toMap
    }.recoverWith { case _: FileNotFoundException => Success(Map.empty[String, String]) }

    for {
      sha1Map <- maybeSha1Map
      _ <- createFileAndFolderSdos(dir, parentSDO, datasetRights, sha1Map)
    } yield ()
  }

  private def createFileAndFolderSdos(dir: File, parentSDO: String, datasetRights: AccessCategory, sha1Map: Map[String, String])(implicit s: Settings): Try[Unit] = {
    logger.debug(s"Creating file and folder SDOs for directory: $dir")

    for {
      child <- Try { dir.listFiles().toList }
      _ <- child.map {
        case file if file.isFile => createFileSdo(file, parentSDO, datasetRights, sha1Map)
        case directory if directory.isDirectory => createFolderSdo(directory, parentSDO)
          .flatMap(_ => createFileAndFolderSdos(directory, getSDODir(directory).getName, datasetRights, sha1Map))
        case other => Failure(new RuntimeException(s"Unknown object encountered while traversing ${ dir.getName }: ${ other.getName }"))
      }.collectResults.map(_ => ())
    } yield ()
  }

  private def getBagRelativePath(path: Path)(implicit s: Settings): Path = {
    s.bagitDir.toPath.relativize(path)
  }

  private def createFileSdo(file: File, parentSDO: String, datasetRights: AccessCategory, sha1Map: Map[String, String])(implicit s: Settings): Try[Unit] = {
    logger.debug(s"Creating file SDO for $file")
    val datasetRelativePath = getDatasetRelativePath(file)
    for {
      sdoDir <- mkdirSafe(getSDODir(file))
      bagRelativePath = s.bagitDir.toPath.relativize(file.toPath).toString
      fileMetadata <- readFileMetadata(bagRelativePath)
      mime <- readMimeType(fileMetadata)
      title <- readTitle(fileMetadata)
      fileAccessRights <- getFileAccessRights(fileMetadata, datasetRights)
      fileVisibleToRights <- getFileVisibleToRights(fileMetadata, datasetRights)
      _ <- FileItemSettings(
        sdoSetDir = s.sdoSetDir,
        file = s.fileUris.get(getBagRelativePath(file.toPath)).fold(Option(file))(_ => Option.empty),
        datastreamLocation = s.fileUris.get(getBagRelativePath(file.toPath)).map(_.toURL),
        ownerId = s.ownerId,
        pathInDataset = datasetRelativePath.toFile,
        size = Some(file.length),
        format = Some(mime),
        sha1 = sha1Map.get(bagRelativePath), // first get is checked in advance
        title = title,
        accessibleTo = fileAccessRights,
        visibleTo = fileVisibleToRights,
        databaseUrl = s.databaseUrl,
        databaseUser = s.databaseUser,
        databasePassword = s.databasePassword)
        .map(EasyStageFileItem.createFileSdo(sdoDir, SdoRelationObject(new File(parentSDO)))(_))
        .tried
    } yield ()
  }

  private def getFileAccessRights(fileMetadata: NodeSeq, datasetRights: AccessCategory)(implicit s: Settings): Try[FileAccessRights.Value] = {
    readAccessRights(fileMetadata).map(_.flatMap(FileAccessRights.valueOf).getOrElse(FileAccessRights.accessibleTo(datasetRights)))
  }

  private def getFileVisibleToRights(fileMetadata: NodeSeq, datasetRights: AccessCategory): Try[FileAccessRights.Value] = {
    readVisibleToRights(fileMetadata).map(_.flatMap(FileAccessRights.valueOf).getOrElse(FileAccessRights.visibleTo(datasetRights)))
  }

  private def createFolderSdo(folder: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    logger.debug(s"Creating folder SDO for $folder")
    lazy val relativePath = getDatasetRelativePath(folder).toString
    for {
      sdoDir <- mkdirSafe(getSDODir(folder))
      _ <- FileItemSettings(
        s.sdoSetDir,
        s.ownerId,
        getDatasetRelativePath(folder).toFile,
        s.databaseUrl,
        s.databaseUser,
        s.databasePassword)
        .map(EasyStageFileItem.createFolderSdo(sdoDir, relativePath, SdoRelationObject(new File(parentSDO)))(_))
        .tried
    } yield ()
  }

  private def getSDODir(fileOrDir: File)(implicit s: Settings): File = {
    val sdoName = getDatasetRelativePath(fileOrDir).toString.replace("/", "_").replace(".", "_") match {
      case name if name startsWith "_" => name.tail
      case name => name
    }
    new File(s.sdoSetDir.getPath, sdoName)
  }

  private def getDatasetRelativePath(item: File)(implicit s: Settings): Path = {
    new File(s.bagitDir, "data").toPath.relativize(item.toPath)
  }
}
