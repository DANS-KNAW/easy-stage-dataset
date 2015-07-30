package nl.knaw.dans.easy.stage

import java.io.File

import scala.util.{Success, Failure, Try}
import scala.xml.XML

object Util {

  class CompositeException(throwables: Seq[Throwable]) extends RuntimeException(throwables.foldLeft("")((msg, t) => s"$msg\n${t.getMessage}"))

  implicit class TryExtensions(xs: Seq[Try[Unit]]) {
    def allSuccess: Try[Unit] =
      if (xs.exists(_.isFailure))
        Failure(new CompositeException(xs.collect { case Failure(e) => e }))
      else
        Success(Unit)
  }

  def mkdirSafe(f: File): Try[File] = Try {
    f.mkdir()
    f
  }

  def writeToFile(f: File, s: String): Try[Unit] =
    Try { scala.tools.nsc.io.File(f).writeAll(s) }

  def readMimeType(filePath: String)(implicit s: Settings): Try[String] = Try {
    val filesMetadata = new File(s.bagitDir, "metadata/files.xml")
    if (!filesMetadata.exists) {
      throw new RuntimeException("Unable to find `metadata/files.xml` in bag.")
    }
    val mimes = for {
      file <- XML.loadFile(filesMetadata) \\ "files" \ "file"
      if (file \ "@filepath").text == filePath
      mime <- file \ "format"
    } yield mime
    if (mimes.size != 1)
      throw new RuntimeException(s"Filepath [$filePath] doesn't exist in files.xml, or isn't unique.")
    mimes(0).text
  }

  def readAudiences()(implicit s: Settings): Try[Seq[String]] = Try {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists) {
      throw new RuntimeException("Unable to find `metadata/dataset.xml` in bag.")
    }
    for {
      audience <- XML.loadFile(ddm) \\ "DDM" \ "profile" \ "audience"
    } yield audience.text
  }

  def getSDODir(fileOrDir: File)(implicit s: Settings): File = {
    val sdoName = fileOrDir.getPath.replace(s.bagitDir.getPath, "").replace("/", "_").replace(".", "_") match {
      case name if name.startsWith("_") => name.tail
      case name => name
    }
    new File(s.sdoSetDir.getPath, sdoName)
  }

}
