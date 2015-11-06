package nl.knaw.dans.easy.stage.lib

import java.io.File

import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import org.slf4j.LoggerFactory

import scala.util.Try

object Util {
  val log = LoggerFactory.getLogger(getClass)

  def writeToFile(f: File, s: String): Try[Unit] =
    Try { scala.tools.nsc.io.File(f).writeAll(s) }

  def mkdirSafe(f: File): Try[File] = Try {
    log.debug(s"creating dir $f")
    f.mkdir()
    f
  }
}
