/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.dataset

import java.io.File

import nl.knaw.dans.easy.stage.{RejectedDepositException, Settings}
import nl.knaw.dans.easy.stage.lib.Util.loadXML

import scala.sys.error
import scala.util.Try
import scala.xml.Elem

object Util {

  class CompositeException(throwables: Seq[Throwable])
    extends RuntimeException(throwables.foldLeft("Multiple failures:")((msg, t) => s"$msg\n${t.getClass}: ${t.getMessage}, ${getFirstDansFrame(t)}"))

  private def getFirstDansFrame(t: Throwable): String = {
    if(t.getStackTrace.length > 0) {
      val st = t.getStackTrace
      st.find(_.getClassName.contains("nl.knaw.dans")) match {
        case Some(el) => s"${el.getClassName}.${el.getMethodName} (${el.getFileName}, ${el.getLineNumber})"
        case None => "<No DANS code in stacktrace ?>"
      }
    }
    else "<Unknown error location>"
  }

  def readMimeType(filePath: String)(implicit s: Settings): Try[String] = Try {
    val mimes = for {
      file <- loadBagXML("metadata/files.xml") \\ "files" \ "file"
      if (file \ "@filepath").text == filePath
      mime <- file \ "format"
    } yield mime
    if (mimes.size != 1)
      throw RejectedDepositException(s"Filepath [$filePath] doesn't exist in files.xml, or isn't unique.")
    mimes.head.text
  }

  def readTitle(filePath: String)(implicit s: Settings): Try[Option[String]] = Try {
    val titles = for {
      file <- loadBagXML("metadata/files.xml") \\ "files" \ "file"
      if (file \ "@filepath").text == filePath
      title <- file \ "title"
    } yield title
    if(titles.size == 1) Option(titles.head.text)
    else None
  }

  def readAudiences()(implicit s: Settings): Try[Seq[String]] = Try {
    for {
      audience <- loadBagXML("metadata/dataset.xml") \\ "DDM" \ "profile" \ "audience"
    } yield audience.text
  }

  def loadBagXML(fileName: String)(implicit s: Settings): Elem = {
    val metadataFile = new File(s.bagitDir, fileName)
    if (!metadataFile.exists) {
      error(s"Unable to find `$fileName` in bag.")
    }
    loadXML(metadataFile)
  }
}
