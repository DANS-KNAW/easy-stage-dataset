package nl.knaw.dans.easy.stage.fileitem

import java.io.File

import nl.knaw.dans.easy.stage.lib.FOXML.{getDirFOXML,getFileFOXML}
import nl.knaw.dans.easy.stage.lib.Props.props
import nl.knaw.dans.easy.stage.lib.Util.mkdirSafe
import nl.knaw.dans.easy.stage.lib._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyStageFileItem {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val conf = new FileItemConf(args)
    if (conf.datasetId.isDefined) {
      Array(FileItemSettings(conf))
    } else {
      FileItemCsv.read(System.in,conf).get
    }.foreach { fileItemSettings =>
      run(fileItemSettings) match {
        case Success(_) => log.info("Staging SUCCESS")
        case Failure(t) => log.error("Staging FAIL", t)
      }
    }
  }

  def run(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"settings = $s")
    for {
      _       <- datasetExists
      _       <- mkdirSafe(s.sdoSetDir)
      dataDir <- mkdirSafe(new File(s.sdoSetDir, s.datasetId.replace(":", "_")))
      _       <- if (s.file.isDefined) createFileSdo(s.file.get, s.ownerId)
                 else createFolderSdo(s.filePath,s.ownerId)
    } yield ()
  }

  def datasetExists(implicit s: FileItemSettings): Try[Unit] = Try {
      if ( Fedora.findObjects(s"pid~${s.datasetId}").isEmpty)
        throw new RuntimeException(s"Dataset not found: ${s.datasetId}")
  }

  def findParent(implicit s: FileItemSettings): Try[String] = {
    val parentFile = s.filePath.getParentFile
    if (parentFile.toString=="") Success(s.datasetId)
    else EasyFilesAndFolders.getFolder(parentFile, s.datasetId)
  }

  private def createFileSdo(file: File, ownerId: String)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating file SDO for $file")
    for {
      parentId <- findParent
      mime     <- Try{s.format.get}
      sdoDir   <- mkdirSafe(getSDODir())
      _        =  FileUtils.copyFileToDirectory(file, sdoDir)
      _        <- JSON.createFileCfg(s.file.get.getAbsolutePath, mime, sdoDir, parentId)
      _        <- FOXML.create(sdoDir, getFileFOXML(file.getName, ownerId, mime))
      _        <- EasyFileMetadata.create(sdoDir, file, mime, s.filePath.toString)
    } yield ()
  }

  private def createFolderSdo(folder: File, ownerId: String)(implicit s: FileItemSettings): Try[Unit] = {
    log.debug(s"Creating folder SDO for $folder")
    for {
      parentId <- findParent
      sdoDir   <- mkdirSafe(getSDODir())
      _        <- JSON.createDirCfg(folder.getName, sdoDir, parentId)
      _        <- FOXML.create(sdoDir, getDirFOXML(folder.getName, ownerId))
      _        <- EasyItemContainerMd.create(sdoDir, folder, relativePath = s.filePath.toString)
    } yield ()
  }

  private def getSDODir()(implicit s: FileItemSettings): File = {
    val sdoName = s.filePath.toString.replaceAll("[/.]", "_").replaceAll("^_","")
    new File(s.sdoSetDir.getPath, sdoName)
  }
}
