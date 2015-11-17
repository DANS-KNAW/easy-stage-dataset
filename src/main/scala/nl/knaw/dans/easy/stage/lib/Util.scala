package nl.knaw.dans.easy.stage.lib

import java.io.File

import org.slf4j.LoggerFactory

import scala.util.{Failure, Try}

object Util {
  val log = LoggerFactory.getLogger(getClass)

  def writeToFile(f: File, s: String): Try[Unit] =
    Try { scala.tools.nsc.io.File(f).writeAll(s) }

  def writeJsonCfg(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "cfg.json"), content)

  def writeFoxml(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "fo.xml"), content)

  def writePrsql(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "PRSQL"), content)

  def writeAMD(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "AMD"), content)

  def writeEMD(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "EMD"), content)

  def writeFileMetadata(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "EASY_FILE_METADATA"), content)

  def writeItemContainerMetadata(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "EASY_ITEM_CONTAINER_MD"), content)

  def mkdirSafe(f: File): Try[File] = Try {
    log.debug(s"creating dir $f")
    f.mkdir()
    f
  }

  def mkdirSafe(f: Option[File]): Try[File] =
    if (f.isEmpty) Failure(new Exception("no file provided"))
    else Try {
      log.debug(s"creating dir ${f.get}")
      f.get.mkdir()
      f.get
    }
}
