package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.FOXML._
import nl.knaw.dans.easy.stage.Util._
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Try}

object Main {

  def main(args: Array[String]) {

    implicit val s = Settings(
      ownerId = "georgi",
      bagStorageLocation = "http://localhost/bags",
      bagitDir = new File("test-resources/example-bag"),
      sdoSetDir = new File("out/sdoSetDir"))

    val dataDir = s.bagitDir.listFiles.find(_.getName == "data")
      .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))

    createDatasetSDO().flatMap(_ => createSDOs(dataDir, DATASET_SDO)).get
  }

  def createDatasetSDO()(implicit s: Settings): Try[Unit] =
    for {
      sdoDir <- mkdirSafe(new File(s.sdoSetDir, DATASET_SDO))
      _ <- AMD.create(sdoDir)
      emd <- EMD.create(sdoDir)
      _ <- FOXML.create(sdoDir, getDatasetFOXML(s.ownerId, emd))
      _ <- PRSQL.create(sdoDir)
    } yield ()

  def createSDOs(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    def visit(child: File): Try[Unit] =
      if (child.isFile)
        createFileSDO(child, parentSDO)
      else if (child.isDirectory)
        createDirSDO(child, parentSDO).flatMap(_ => createSDOs(child, child.getName))
      else
        Failure(new RuntimeException(s"Unknown object encountered while traversing ${dir.getName}: ${child.getName}"))
    Try { dir.listFiles().toList }.flatMap(_.map(visit).allSuccess)
  }

  def createFileSDO(file: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    val relativePath = file.getPath.replaceFirst(s.bagitDir.getPath, "").substring(1)
    for {
      sdoDir <- mkdirSafe(getSDODir(file))
      mime <- readMimeType(relativePath)
      _ = FileUtils.copyFileToDirectory(file, sdoDir)
      _ <- JSON.createFileCfg(s"${s.bagStorageLocation}/$relativePath", mime, parentSDO, sdoDir)
      _ <- FOXML.create(sdoDir, getFileFOXML(file.getName, s.ownerId, mime))
    } yield ()
  }

  def createDirSDO(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] =
    for {
      sdoDir <- mkdirSafe(getSDODir(dir))
      _ <- JSON.createDirCfg(dir.getName, parentSDO, sdoDir)
      _ <- FOXML.create(sdoDir, getDirFOXML(dir.getName, s.ownerId))
    } yield ()

}
