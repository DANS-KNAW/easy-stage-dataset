package nl.knaw.dans.easy.stage

import java.io.File

import scala.util.{Success, Failure, Try}
import scala.xml.{Elem, XML}

object Util {

  class CompositeException(throwables: Seq[Throwable]) extends RuntimeException(throwables.foldLeft("")((msg, t) => s"$msg\n${t.getMessage}"))

  implicit class TryExtensions(xs: Seq[Try[Unit]]) {
    def allSuccess: Try[Unit] =
      if (xs.exists(_.isFailure))
        Failure(new CompositeException(xs.collect { case Failure(e) => e }))
      else
        Success(Unit)
  }

  def readMimeType(filePath: String)(implicit s: Settings): Try[String] = Try {
    val mimes = for {
      file <- loadXML("metadata/files.xml") \\ "files" \ "file"
      if (file \ "@filepath").text == filePath
      mime <- file \ "format"
    } yield mime
    if (mimes.size != 1)
      throw new scala.RuntimeException(s"Filepath [$filePath] doesn't exist in files.xml, or isn't unique.")
    mimes(0).text
  }

  def readAudiences()(implicit s: Settings): Try[Seq[String]] = Try {
    for {
      audience <- loadXML("metadata/dataset.xml") \\ "DDM" \ "profile" \ "audience"
    } yield audience.text
  }

  private def loadXML(fileName: String)(implicit s: Settings): Elem = {
    val metadataFile = new File(s.bagitDir, fileName)
    if (!metadataFile.exists) {
      throw new scala.RuntimeException(s"Unable to find `$fileName` in bag.")
    }
    XML.loadFile(metadataFile)
  }
}
