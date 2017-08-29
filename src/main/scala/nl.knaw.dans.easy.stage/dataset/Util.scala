/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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

import nl.knaw.dans.easy.stage.lib.Util.loadXML
import nl.knaw.dans.easy.stage.{ RejectedDepositException, Settings }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.sys.error
import scala.util.Try
import scala.xml.{ Elem, NodeSeq }

object Util extends DebugEnhancedLogging {

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
    mimes match {
      case Seq(mime) => mime.text
      case _ => throw RejectedDepositException(s"format element doesn't exist for the file, or isn't unique.")
    }
  }

  def readTitle(fileMetadata: NodeSeq)(implicit s: Settings): Try[Option[String]] = Try {
    fileMetadata \ "title" match {
      case Seq(title) => Option(title.text)
      case _ => None
    }
  }

  def readAccessRights(fileMetadata: NodeSeq)(implicit s: Settings): Try[Option[String]] = Try {
    fileMetadata \ "accessRights" match {
      case Seq(right) => Option(right.text)
      case _ => None
    }
  }

  def readAudiences()(implicit s: Settings): Try[Seq[String]] = Try {
    trace(())
    (loadBagXML("metadata/dataset.xml") \\ "DDM" \ "profile" \ "audience").map(_.text)
  }

  def loadBagXML(fileName: String)(implicit s: Settings): Elem = {
    new File(s.bagitDir, fileName) match {
      case file if file.exists() => loadXML(file)
      case _ => error(s"Unable to find `$fileName` in bag.")
    }
  }
}
