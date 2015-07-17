package nl.knaw.dans.easy.stage

import java.io.File
import java.nio.file.Paths

import scala.util.Try

object Main {

  case class Settings(bagitDir: File, sdoSetDir: File)

  def main(args: Array[String]) {
    implicit val s = Settings(
      bagitDir = new File("test-resources/example-bag"),
      sdoSetDir = new File("out/sdoSetDir"))

    val dataDir = s.bagitDir.listFiles.find(_.getName == "data")
      .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))
    createDigitalObjects(dataDir)
  }

  def createDigitalObjects(dataDir: File)(implicit s: Settings): Try[Unit] = Try {
    val children = dataDir.listFiles()
    children.foreach(child => {
      if (child.isFile) {
        createFileDO(child)
      } else if (child.isDirectory) {
        createDirDO(child)
        createDigitalObjects(child)
      }
    })
  }

  def createFileDO(file: File)(implicit s: Settings): Try[Unit] = Try {
    val sdoDir = getDODir(file)
    println("file DO> " + sdoDir.getPath)
    sdoDir.mkdir()
  }

  def createDirDO(dir: File)(implicit s: Settings): Try[Unit] = Try {
    val sdoDir = getDODir(dir)
    println("dir DO> " + sdoDir.getPath)
    sdoDir.mkdir()
  }

  def getDODir(fileOrDir: File)(implicit s: Settings): File = {
    val sdoName = fileOrDir.getPath.replace(s.bagitDir.getPath, "").replace("/", "_").replace(".", "_") match {
      case name if name.startsWith("_") => name.tail
      case name => name
    }
    Paths.get(s.sdoSetDir.getPath, sdoName).toFile
  }
}
