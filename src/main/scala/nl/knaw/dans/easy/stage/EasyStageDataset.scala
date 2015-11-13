package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.dataset.Util._
import nl.knaw.dans.easy.stage.dataset.{AMD, AdditionalLicense, EMD, PRSQL}
import nl.knaw.dans.easy.stage.fileitem.{EasyStageFileItem, FileItemSettings}
import nl.knaw.dans.easy.stage.lib.Constants._
import nl.knaw.dans.easy.stage.lib.FOXML._
import nl.knaw.dans.easy.stage.lib.JSON
import nl.knaw.dans.easy.stage.lib.Util._
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyStageDataset {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
    val conf = new Conf(args)

    implicit val s = Settings(
      ownerId = props.getString("owner"),
      submissionTimestamp = conf.submissionTimestamp.apply().toString,
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
      _ <- writeAMD(sdoDir, AMD(s.ownerId, s.submissionTimestamp).toString())
      emd <- EMD.create(sdoDir)
      _ <- writeFoxml(sdoDir, getDatasetFOXML(s.ownerId, emd))
      _ <- writePrsql(sdoDir, PRSQL.create())
      license <- AdditionalLicense.create(sdoDir)
      audiences <- readAudiences()
      _ <- writeJsonCfg(sdoDir, JSON.createDatasetCfg(license, audiences))
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
      _ <- EasyStageFileItem.createFileSdo(
        sdoDir = sdoDir,
        parentId = None,
        parentSdoDir = new File(parentSDO)
      )(FileItemSettings(
        sdoSetDir = s.sdoSetDir,
        file = Some(file),
        ownerId = s.ownerId,
        filePath = new File(relativePath),
        format = Some(mime)
      ))
    } yield ()
  }

  private def createFolderSdo(folder: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    log.debug(s"Creating folder SDO for $folder")
    for {
      sdoDir <- mkdirSafe(getSDODir(folder))
      _ <- EasyStageFileItem.createFolderSdo(
        sdoDir = sdoDir,
        parentId = None,
        parentSdoDir = new File(parentSDO)
      )(FileItemSettings(
        sdoSetDir = s.sdoSetDir,
        ownerId = s.ownerId,
        filePath = new File(getRelativePath(folder))
      ))
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
