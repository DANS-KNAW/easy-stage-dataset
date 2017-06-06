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
package nl.knaw.dans.easy.stage.lib

import java.io.{ File, FileInputStream, InputStreamReader }

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.FileUtils

import scala.util.{ Failure, Try }
import scala.xml.{ Elem, XML }

object Util extends DebugEnhancedLogging {
  def loadXML(metadataFile: File): Elem =
    XML.load(new InputStreamReader(new FileInputStream(metadataFile), "UTF-8"))

  def writeToFile(f: File, s: String): Try[Unit] =
    /*
     * DO NOT USE THE SCALA File CLASS TO WRITE THE XML.
     * It does not have a Charset parameter, so it will try to write in the
     * platform's default Charset, resulting in question marks for characters that
     * are not in that Charset.
     *
     * See: https://drivenbydata.atlassian.net/browse/EASY-984
     */
    Try { FileUtils.write(f, s, "UTF-8")}

  def writeJsonCfg(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "cfg.json"), content)

  def writeFoxml(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "fo.xml"), content)

  def writePrsql(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "PRSQL"), content)

  def writeAMD(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "AMD"), content)

  def writeEMD(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "EMD"), content)

  def writeFileMetadata(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "EASY_FILE_METADATA"), content)

  def copyFile(sdoDir: File, file: File): Try[Unit] = Try {
    FileUtils.copyFile(file, new File(sdoDir, "EASY_FILE"))
  }

  def writeItemContainerMetadata(sdoDir: File, content: String): Try[Unit] =
    writeToFile(new File(sdoDir, "EASY_ITEM_CONTAINER_MD"), content)

  def mkdirSafe(f: File): Try[File] = Try {
    logger.debug(s"Creating dir $f")
    f.mkdirs()
    f
  }

  def mkdirSafe(f: Option[File]): Try[File] =
    if (f.isEmpty) Failure(new Exception("no file provided"))
    else Try {
      logger.debug(s"Creating dir ${f.get}")
      f.get.mkdirs()
      f.get
    }
}
