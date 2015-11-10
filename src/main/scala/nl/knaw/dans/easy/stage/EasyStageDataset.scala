package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.dataset.{PRSQL, EMD, AMD, AdditionalLicense}
import nl.knaw.dans.easy.stage.lib._
import Constants._
import nl.knaw.dans.easy.stage.lib.{EasyItemContainerMd, EasyFileMetadata, JSON, FOXML}
import FOXML._
import dataset.Util._
import nl.knaw.dans.easy.stage.lib.Util._
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyStageDataset {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
    val conf = new Conf(args)

    implicit val s = Settings(
      ownerId = props.getString("owner"),
      submissionTimestamp = conf.submissionTimestamp().toString,
      bagStorageLocation = props.getString("storage-base-url"),
      bagitDir = conf.bag(),
      sdoSetDir = conf.sdoSet(),
      URN = conf.urn(),
      DOI = conf.doi(),
      otherAccessDOI = conf.otherAccessDOI(),
      fedoraUser = props.getString("fcrepo-user"),
      fedoraPassword = props.getString("fcrepo-password"),
      fedoraUrl = new URL(props.getString("fcrepo-service-url")))

    run match {
      case Success(_) => log.info("Staging SUCCESS")
      case Failure(t) => log.error("Staging FAIL", t)
    }
  }

  def run(implicit s: Settings): Try[Unit] = {
    log.debug(s"settings = $s")
    for {
      dataDir <- getDataDir
      _ <- mkdirSafe(s.sdoSetDir)
      _ <- createDatasetSdo()
      _ = log.info("Creating file and folder SDOs")
      _ <- createFileAndFolderSdos(dataDir, DATASET_SDO)
    } yield ()
  }

  private def createDatasetSdo()(implicit s: Settings): Try[Unit] = {
    log.info("Creating dataset SDO")
    for {
      sdoDir <- mkdirSafe(new File(s.sdoSetDir, DATASET_SDO))
      _ <- AMD.create(sdoDir)
      emd <- EMD.create(sdoDir)
      _ <- FOXML.create(sdoDir, getDatasetFOXML(s.ownerId, emd))
      _ <- PRSQL.create(sdoDir)
      license <- AdditionalLicense.create(sdoDir)
      _ <- JSON.createDatasetCfg(sdoDir, license)
    } yield ()
  }

  private def createFileAndFolderSdos(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    log.debug(s"Creating file and folder SDOs for directory: $dir")
    def visit(child: File): Try[Unit] =
      if (child.isFile)
        createFileSdo(child, parentSDO)
      else if (child.isDirectory)
        createFolderSdo(child, parentSDO).flatMap(_ => createFileAndFolderSdos(child, getSDODir(child).getName))
      else
        Failure(new RuntimeException(s"Unknown object encountered while traversing ${dir.getName}: ${child.getName}"))
    Try { dir.listFiles().toList }.flatMap(_.map(visit).allSuccess)
  }

  private def createFileSdo(file: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    log.debug(s"Creating file SDO for $file")
    val relativePath = file.getPath.replaceFirst(s.bagitDir.getPath, "").substring(1)
    for {
      sdoDir <- mkdirSafe(getSDODir(file))
      mime <- readMimeType(relativePath)
      _ = FileUtils.copyFileToDirectory(file, sdoDir)
      _ <- JSON.createFileCfg(s"${s.bagStorageLocation}/$relativePath", mime, parentSDO, sdoDir)
      _ <- FOXML.create(sdoDir, getFileFOXML(file.getName, s.ownerId, mime))
      _ <- EasyFileMetadata.create(sdoDir, file, mime, getRelativePath(file))
    } yield ()
  }

  private def createFolderSdo(folder: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    log.debug(s"Creating folder SDO for $folder")
    for {
      sdoDir <- mkdirSafe(getSDODir(folder))
      _ <- JSON.createDirCfg(folder.getName, parentSDO, sdoDir)
      _ <- FOXML.create(sdoDir, getDirFOXML(folder.getName, s.ownerId))
      _ <- EasyItemContainerMd.create(sdoDir, folder, getRelativePath(folder))
    } yield ()
  }

  private def getDataDir(implicit s: Settings) = Try {
    s.bagitDir.listFiles.find(_.getName == "data")
      .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))
  }

  def getSDODir(fileOrDir: File)(implicit s: Settings): File = {
    val sdoName = getRelativePath(fileOrDir).replace("/", "_").replace(".", "_") match {
      case name if name.startsWith("_") => name.tail
      case name => name
    }
    new File(s.sdoSetDir.getPath, sdoName)
  }

  def getRelativePath(fileOrDir: File)(implicit s: Settings): String =
    fileOrDir.getPath.replace(s.bagitDir.getPath, "")
}
