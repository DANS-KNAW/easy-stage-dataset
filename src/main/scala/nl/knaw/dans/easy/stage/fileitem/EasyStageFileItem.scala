package nl.knaw.dans.easy.stage.fileitem

import java.io.{FileInputStream, File}
import java.sql.SQLException

import com.yourmediashelf.fedora.client.FedoraClientException
import nl.knaw.dans.easy.stage.lib.Constants._
import nl.knaw.dans.easy.stage.lib.FOXML.{getDirFOXML,getFileFOXML}
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.easy.stage.lib._
import org.apache.commons.io.FileUtils
import org.json4s.native.JsonMethods._
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyStageFileItem {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val conf = new FileItemConf(args)
    (if (conf.datasetId.isDefined) {
      Array(FileItemSettings(conf))
    } else {
      if (conf.csvFile.isEmpty)
        throw new Exception("neither datasetId (option -i) nor CSV file (optional trail argument) specified")
      FileItemCsv.read(new FileInputStream(conf.csvFile.apply()), conf).get
    }).foreach { fileItemSettings =>
      run(fileItemSettings) match {
        case Success(_) => log.info(s"Staging SUCCESS of $fileItemSettings")
        case Failure(t) => log.error(s"Staging FAIL of $fileItemSettings", t)
          if(t.isInstanceOf[SQLException] || t.isInstanceOf[FedoraClientException]) return
      }
    }
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
      parentSdoDir  =  if(s.filePath.getParentFile == null) datasetSdoDir
                       else new File(datasetSdoDir, toSdoName(s.filePath.getParentFile))
      _             <- if (parentId.isDefined || parentSdoDir.isDirectory) Success()
                       else Failure(new scala.Exception(s"${parentSdoDir.getName} was not staged"))
      _             <- if (s.file.isDefined) createFileSdo(sdoDir, parentId, parentSdoDir)
                       else createFolderSdo(sdoDir, parentId, parentSdoDir)
    } yield ()
  }

  def createFileSdo(sdoDir: File, parentId: Option[String], parentSdoDir: File)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating file SDO: ${s.filePath}")
    val fmFile    = new File(sdoDir.getPath, EASY_FILE_METADATA_FILENAME)
    val jsonFile  = new File(sdoDir.getPath, JSON_CFG_FILENAME)
    val foxmlFile = new File(sdoDir.getPath, FOXML_FILENAME)
    val location  = new File(s.storageBaseUrl, s.filePath.toString).toString
    for {
      mime <- Try{s.format.get}
      _ <- Try{FileUtils.copyFileToDirectory(s.file.get, sdoDir)}
      _ <- writeToFile(jsonFile,pretty(render(if (parentId.isDefined)
                JSON.createFileCfg(location, mime, parentId.get)
           else JSON.createFileCfg(location, mime, parentSdoDir))))
      _ <- writeToFile(foxmlFile, getFileFOXML(s.filePath.getName, s.ownerId, mime))
      _ <- writeToFile(fmFile, EasyFileMetadata(s).toString())
    } yield ()
  }

  def createFolderSdo(sdoDir: File, parentId: Option[String], parentSdoDir: File)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating folder SDO: ${s.filePath}")
    val jsonFile  = new File(sdoDir.getPath, JSON_CFG_FILENAME)
    for {
      _ <- writeToFile(jsonFile,pretty(render(if (parentId.isDefined)
                JSON.createDirCfg(s.filePath.getName, parentId.get)
           else JSON.createDirCfg(s.filePath.getName, parentSdoDir))))
      _ <- FOXML.create(sdoDir, getDirFOXML(s.filePath.getName, s.ownerId))
      _ <- EasyItemContainerMd.create(sdoDir, s.filePath, relativePath = s.filePath.toString)
    } yield ()
  }

  private def toSdoName(path: File): String =
    path.toString.replaceAll("[/.]", "_").replaceAll("^_", "")
}
