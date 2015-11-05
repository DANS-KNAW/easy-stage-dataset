package nl.knaw.dans.easy.stage.lib

import java.io.File

import scala.util.Try

object Util {

  def writeToFile(f: File, s: String): Try[Unit] =
    Try { scala.tools.nsc.io.File(f).writeAll(s) }

  def mkdirSafe(f: File): Try[File] = Try {
    f.mkdir()
    f
  }
}
