/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import scala.xml.{Elem, NodeSeq}

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

  /**
   * Load file metadata XML file and extract the metadata for the specified file.
   * Use this as input for further processing and extraction of sub-elements like title and mime type.
   *
   * @param filePath Path to the file, relative to the bag
   * @param s Settings
   * @return File metadata (XML Nodes) for the specified file
   */
  def readFileMetadata(filePath: String)(implicit s: Settings): Try[NodeSeq] = Try {
    for {
      file <- loadBagXML("metadata/files.xml") \\ "files" \ "file"
      if (file \@ "filepath") == filePath
    } yield file
  }

  def readMimeType(fileMetadata: NodeSeq)(implicit s: Settings): Try[String] = Try {
    val mimes =  fileMetadata \ "format"
    if (mimes.size != 1)
      throw RejectedDepositException(s"format element doesn't exist for the file, or isn't unique.")
    mimes.head.text
  }

  def readTitle(fileMetadata: NodeSeq)(implicit s: Settings): Try[Option[String]] = Try {
    val titles = fileMetadata \ "title"
    if(titles.size == 1) Option(titles.head.text)
    else None
  }

  def readAccessRights(fileMetadata: NodeSeq)(implicit s: Settings): Try[Option[String]] = Try {
    val rights = fileMetadata \ "accessRights"
    if(rights.size == 1) Option(rights.head.text)
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
