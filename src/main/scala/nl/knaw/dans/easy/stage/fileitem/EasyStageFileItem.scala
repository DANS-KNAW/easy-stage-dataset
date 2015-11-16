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
    getConfs(conf) match {
      case Failure(t) => log.error(s"Staging FAIL", t)
      case Success(seq) => if (seq.isEmpty) log.error(s"Empty CSV file") else
        seq.foreach { conf =>
          val settings = FileItemSettings(conf)
          run(settings) match {
            case Success(_) => log.info(s"Staging SUCCESS of $settings")
            case Failure(t) => log.error(s"Staging FAIL of $settings", t)
              if (t.isInstanceOf[SQLException] || t.isInstanceOf[FedoraClientException]) return
          }
        }
    }
  }

  def getConfs(conf: FileItemConf): Try[Seq[FileItemConf]] =
    if (conf.datasetId.isDefined)
      Success(Seq(conf))
    else if (conf.csvFile.isEmpty)
      Failure(new Exception("neither datasetId (option -i) nor CSV file (optional trail argument) specified"))
    else {
      val trailArgs = Array(conf.sdoSetDir.apply().toString)
      val in = new FileInputStream(conf.csvFile.apply())
      CSV(in, conf).flatMap(argsList => Success(argsList.map(
          args => new FileItemConf(args ++ trailArgs)
      )))
    }

  def run(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"executing: $s")
    for {
      _             <- if (Fedora.findObjects(s"pid~${s.datasetId}").nonEmpty) Success(Unit)
                       else Failure(new Exception(s"${s.datasetId} does not exist"))
      parentId      <- if(s.filePath.getParentFile == null) Success(Some(s.datasetId))
                       else EasyFilesAndFolders.getPathId(s.filePath.getParentFile, s.datasetId)
      _             <- mkdirSafe(s.sdoSetDir)
      datasetSdoDir <- mkdirSafe(new File(s.sdoSetDir, s.datasetId.replace(":", "_")))
      sdoDir        <- mkdirSafe(new File(datasetSdoDir, toSdoName(s.filePath)))
      parentSdoDir  =  Option(s.filePath.getParentFile).map(f => new File(datasetSdoDir, toSdoName(f))).getOrElse(datasetSdoDir)
      _             <- if (parentId.isDefined || parentSdoDir.isDirectory) Success(Unit)
                       else Failure(new scala.Exception(s"${parentSdoDir.getName} was not staged"))
      _             <- if (s.file.isDefined) createFileSdo(sdoDir, parentId, parentSdoDir)
                       else createFolderSdo(sdoDir, parentId, parentSdoDir)
    } yield ()
  }

  def createFileSdo(sdoDir: File, parentId: Option[String], parentSdoDir: File)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating file SDO: ${s.filePath}")
    val location  = new File(s.storageBaseUrl, s.filePath.toString).toString
    for {
      mime <- Try{s.format.get}
      _ <- Try{FileUtils.copyFileToDirectory(s.file.get, sdoDir)}
      _ <- writeJsonCfg(sdoDir,if (parentId.isDefined)
                JSON.createFileCfg(location, mime, parentId.get)
           else JSON.createFileCfg(location, mime, parentSdoDir))
      _ <- writeFoxml(sdoDir, getFileFOXML(s.filePath.getName, s.ownerId, mime))
      _ <- writeFileMetadata(sdoDir, EasyFileMetadata(s).toString())
    } yield ()
  }

  def createFolderSdo(sdoDir: File, parentId: Option[String], parentSdoDir: File)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating folder SDO: ${s.filePath}")
    for {
      _ <- writeJsonCfg(sdoDir,if (parentId.isDefined)
                JSON.createDirCfg(s.filePath.getName, parentId.get)
           else JSON.createDirCfg(s.filePath.getName, parentSdoDir))
      _ <- writeFoxml(sdoDir, getDirFOXML(s.filePath.getName, s.ownerId))
      _ <- writeItemContainerMetadata(sdoDir,EasyItemContainerMd(s.filePath))
    } yield ()
  }

  private def toSdoName(path: File): String =
    path.toString.replaceAll("[/.]", "_").replaceAll("^_", "")
}
