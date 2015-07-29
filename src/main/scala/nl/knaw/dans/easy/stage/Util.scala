package nl.knaw.dans.easy.stage

import java.io.File

import scala.util.Try

object Util {

  def mkdirSafe(f: File): Try[File] = Try {
    f.mkdir()
    f
  }

  def writeToFile(f: File, s: String): Try[Unit] =
    Try { scala.tools.nsc.io.File(f).writeAll(s) }

}
