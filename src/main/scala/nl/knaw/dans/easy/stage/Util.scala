package nl.knaw.dans.easy.stage

import java.io.File

import scala.util.Try

object Util {
  def mkdirSafe(f: File): Try[File] = Try {
    f.mkdir()
    f
  }
}
