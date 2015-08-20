package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.FOXML._
import nl.knaw.dans.easy.stage.Util._
import org.apache.commons.io.FileUtils
import org.slf4j.LoggerFactory

import scala.util.{Failure, Try}

object EasyStageDataset {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {

    implicit val s = Settings(
      ownerId = "georgi",
      bagStorageLocation = "http://localhost/bags",
      bagitDir = new File(args(0)),
      sdoSetDir = new File(args(1)),
      URN = "urn:nbn:nl:ui:13-1337-13",
      DOI = "10.1000/xyz123")

    run().get
  }

  def run()(implicit s: Settings): Try[Unit] =
    for {
      dataDir <- getDataDir
      _ <- createDatasetSDO()
      _ <- createSDOs(dataDir, DATASET_SDO)
    } yield ()

  private def createDatasetSDO()(implicit s: Settings): Try[Unit] =
    for {
      sdoDir <- mkdirSafe(new File(s.sdoSetDir, DATASET_SDO))
      _ <- AMD.create(sdoDir)
      emd <- EMD.create(sdoDir)
      _ <- FOXML.create(sdoDir, getDatasetFOXML(s.ownerId, emd))
      _ <- PRSQL.create(sdoDir)
      _ <- JSON.createDatasetCfg(sdoDir)
    } yield ()

  private def createSDOs(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    def visit(child: File): Try[Unit] =
      if (child.isFile)
        createFileSDO(child, parentSDO)
      else if (child.isDirectory)
        createDirSDO(child, parentSDO).flatMap(_ => createSDOs(child, getSDODir(child).getName))
      else
        Failure(new RuntimeException(s"Unknown object encountered while traversing ${dir.getName}: ${child.getName}"))
    Try { dir.listFiles().toList }.flatMap(_.map(visit).allSuccess)
  }

  private def createFileSDO(file: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    val relativePath = file.getPath.replaceFirst(s.bagitDir.getPath, "").substring(1)
    for {
      sdoDir <- mkdirSafe(getSDODir(file))
      mime <- readMimeType(relativePath)
      _ = FileUtils.copyFileToDirectory(file, sdoDir)
      _ <- JSON.createFileCfg(s"${s.bagStorageLocation}/$relativePath", mime, parentSDO, sdoDir)
      _ <- FOXML.create(sdoDir, getFileFOXML(file.getName, s.ownerId, mime))
      _ <- EasyFileMetadata.create(sdoDir, file, mime)
    } yield ()
  }

  private def createDirSDO(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] =
    for {
      sdoDir <- mkdirSafe(getSDODir(dir))
      _ <- JSON.createDirCfg(dir.getName, parentSDO, sdoDir)
      _ <- FOXML.create(sdoDir, getDirFOXML(dir.getName, s.ownerId))
      _ <- EasyItemContainerMd.create(sdoDir, dir)
    } yield ()

  private def getDataDir(implicit s: Settings) = Try {
    s.bagitDir.listFiles.find(_.getName == "data")
      .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))
  }
}
