package nl.knaw.dans.easy.stage.fileitem

import java.io.File
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

  def run(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"executing: $s")
    for {
      datasetId     <- getValidDatasetId(s)
      parentId      <- getParentId(s.filePath, datasetId)
      sdoSetDir     <- mkdirSafe(s.sdoSetDir)
      datasetSdoDir <- mkdirSafe(new File(sdoSetDir, datasetId.replace(":", "_")))
      sdoDir        <- mkdirSafe(new File(datasetSdoDir, toSdoName(s.filePath.get)))
      parentSdoDir  =  Option(s.filePath.get.getParentFile).map(f => new File(datasetSdoDir, toSdoName(f))).getOrElse(datasetSdoDir)
      _             <- if (parentId.isDefined || parentSdoDir.isDirectory) Success(Unit)
                       else Failure(new scala.Exception(s"${parentSdoDir.getName} was not staged"))
      _             <- if (s.file.isDefined) createFileSdo(sdoDir, parentId, parentSdoDir)
                       else createFolderSdo(sdoDir, parentId, parentSdoDir)
    } yield ()
  }

  def createFileSdo(sdoDir: File, parentId: Option[String], parentSdoDir: File)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating file SDO: ${s.filePath.get}")
    val location  = new File(s.storageBaseUrl, s.filePath.get.toString).toString
    for {
      mime <- Try{s.format.get}
      _ <- Try{FileUtils.copyFileToDirectory(s.file.get, sdoDir)}
      _ <- writeJsonCfg(sdoDir,createFileCfg(parentId, parentSdoDir, location, mime))
      _ <- writeFoxml(sdoDir, getFileFOXML(s.filePath.get.getName, s.ownerId, mime))
      _ <- writeFileMetadata(sdoDir, EasyFileMetadata(s).toString())
    } yield ()
  }

  def createFolderSdo(sdoDir: File, parentId: Option[String], parentSdoDir: File)(implicit s: FileItemSettings): Try[Unit] = {
    val filePath = s.filePath.get
    log.debug(s"Creating folder SDO: ${filePath}")
    for {
      _ <- writeJsonCfg(sdoDir,createDirCfg(parentId, parentSdoDir, filePath.getName))
      _ <- writeFoxml(sdoDir, getDirFOXML(filePath.getName, s.ownerId))
      _ <- writeItemContainerMetadata(sdoDir,EasyItemContainerMd(filePath))
    } yield ()
  }

  private def getConfs(conf: FileItemConf): Try[Seq[FileItemConf]] =
    if (conf.datasetId.isDefined)
      Success(Seq(conf))
    else if (conf.csvFile.isEmpty)
      Failure(new Exception("neither datasetId (option -i) nor CSV file (optional trail argument) specified"))
    else {
      val trailArgs = Array(conf.sdoSetDir.apply().toString)
      CSV(conf.csvFile.get.get, conf).flatMap(argsList => Success(argsList.map(
        args => new FileItemConf(args ++ trailArgs)
      )))
    }

  private def getValidDatasetId(s: FileItemSettings): Try[String] = {
    val id = s.datasetId
    if (id.isEmpty)
      Failure(new Exception(s"no datasetId provided"))
    else if (Fedora.findObjects(s"pid~${id.get}").isEmpty)
      Failure(new Exception(s"${id.get} does not exist"))
    else
      Success(id.get)
  }

  private def getParentId(filePath: Option[File], datasetId: String): Try[Option[String]] = {
    if(filePath.isEmpty)
      Failure(new Exception(s"no filePath provided"))
    else if (filePath.get.getParentFile == null)
      Success(Some(datasetId))
    else
      EasyFilesAndFolders.getPathId(filePath.get.getParentFile, datasetId)
  }

  private def createFileCfg(parentId: Option[String], parentSdoDir: File, location: String, mime: String): String = {
    if (parentId.isDefined)
      JSON.createFileCfg(location, mime, parentId.get)
    else JSON.createFileCfg(location, mime, parentSdoDir)
  }

  private def createDirCfg(parentId: Option[String], parentSdoDir: File, fileName: String): String = {
    if (parentId.isDefined)
      JSON.createDirCfg(fileName, parentId.get)
    else JSON.createDirCfg(fileName, parentSdoDir)
  }

  private def toSdoName(path: File): String =
    path.toString.replaceAll("[/.]", "_").replaceAll("^_", "")
}
