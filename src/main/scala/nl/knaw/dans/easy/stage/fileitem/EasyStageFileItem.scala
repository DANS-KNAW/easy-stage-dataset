package nl.knaw.dans.easy.stage.fileitem

import java.io.{File, FileInputStream}
import java.sql.SQLException

import com.yourmediashelf.fedora.client.FedoraClientException
import nl.knaw.dans.easy.stage.lib.FOXML.{getDirFOXML, getFileFOXML}
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.easy.stage.lib._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyStageFileItem {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val conf = new FileItemConf(args)
    getSettingsSeq(conf) match {
      case Failure(t) => log.error(s"Staging FAIL of $conf", t)
      case Success(seq) => if (seq.isEmpty) log.error(s"Empty CSV file") else
        seq.foreach { settings =>
          run(settings) match {
            case Success(_) => log.info(s"Staging SUCCESS of $settings")
            case Failure(t) => log.error(s"Staging FAIL of $settings", t)
              if (t.isInstanceOf[SQLException] || t.isInstanceOf[FedoraClientException]) return
          }
        }
    }
  }

  def run(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"executing: $s")
    for {
      datasetId        <- getValidDatasetId(s)
      sdoSetDir        <- mkdirSafe(s.sdoSetDir)
      datasetSdoSetDir <- mkdirSafe(new File(sdoSetDir, datasetId.replace(":", "_")))
      sdoDir           <- mkdirSafe(new File(datasetSdoSetDir, toSdoName(s.filePath.get)))
      parent           <- getParent(datasetSdoSetDir)
      _                <- if (s.file.isDefined) createFileSdo(sdoDir, parent)
                          else createFolderSdo(sdoDir, parent)
    } yield ()
  }

  def createFileSdo(sdoDir: File, parent: (String,String))(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating file SDO: ${s.filePath.get}")
    val location  = new File(s.storageBaseUrl, s.filePath.get.toString).toString
    for {
      mime <- Try{s.format.get}
      _ <- Try{FileUtils.copyFileToDirectory(s.file.get, sdoDir)}
      _ <- writeJsonCfg(sdoDir, JSON.createFileCfg(location, mime, parent))
      _ <- writeFoxml(sdoDir, getFileFOXML(s.filePath.get.getName, s.ownerId, mime))
      _ <- writeFileMetadata(sdoDir, EasyFileMetadata(s).toString())
    } yield ()
  }

  def createFolderSdo(sdoDir: File, parent: (String,String))(implicit s: FileItemSettings): Try[Unit] = {
    val filePath = s.filePath.get
    log.debug(s"Creating folder SDO: $filePath")
    for {
      _ <- writeJsonCfg(sdoDir,JSON.createDirCfg(parent))
      _ <- writeFoxml(sdoDir, getDirFOXML(filePath.getName, s.ownerId))
      _ <- writeItemContainerMetadata(sdoDir,EasyItemContainerMd(filePath))
    } yield ()
  }

  private def getSettingsSeq(conf: FileItemConf): Try[Seq[FileItemSettings]] =
    if (conf.datasetId.isDefined)
      Success(Seq(FileItemSettings(conf)))
    else if (conf.csvFile.isEmpty)
      Failure(new Exception("neither datasetId (option -i) nor CSV file (optional trail argument) specified"))
    else {
      val trailArgs = Seq(conf.sdoSetDir.apply().toString)
      CSV(conf.csvFile.apply(), conf.longOptionNames.map(_.toUpperCase)).map {
        case (ignored, csv) =>
          log.warn(s"${conf.csvFile.apply()} ignored columns: ${ignored.mkString(", ")}")
          csv.getRows.map(options => FileItemSettings(options ++ trailArgs))
      }
    }

  private def getValidDatasetId(s: FileItemSettings): Try[String] =
    if (s.datasetId.isEmpty)
      Failure(new Exception(s"no datasetId provided"))
    else if (Fedora.findObjects(s"pid~${s.datasetId.get}").isEmpty)
      Failure(new Exception(s"${s.datasetId.get} does not exist in repository"))
    else
      Success(s.datasetId.get)

  private def getParent(datasetSdoSetDir: File)(implicit s: FileItemSettings): Try[(String,String)] =
    if(s.filePath.isEmpty)
      Failure(new Exception(s"no filePath provided"))
    else if (s.filePath.get.getParentFile == null)
      Success("object" -> s.datasetId.get)
    else
      EasyFilesAndFolders.getPathId(s.filePath.get.getParentFile, s.datasetId.get) match {
        case Failure(t) => Failure(t)
        case Success(Some(fileItemId)) => Success("object" -> fileItemId)
        case Success(None) =>
          val parentSdoDir = new File(datasetSdoSetDir, toSdoName(s.filePath.get.getParentFile))
          if (parentSdoDir.exists()) Success("objectSDO" -> parentSdoDir.getName)
          else Failure(new Exception(s"${parentSdoDir.getName} was not staged"))
        }

  private def toSdoName(path: File): String =
    path.toString.replaceAll("[/.]", "_").replaceAll("^_", "")
}
