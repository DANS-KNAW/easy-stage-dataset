/*
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
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.ExistingAncestor
import nl.knaw.dans.easy.stage.lib.Util.loadXML
import nl.knaw.dans.easy.stage.lib.{ Fedora, FedoraRelationObject }
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{ BeforeAndAfterEach, Inside }

import scala.collection.immutable.HashMap
import scala.util.{ Failure, Success, Try }

class EasyStageFileItemSpec extends AnyFlatSpec with Matchers with Inside with BeforeAndAfterEach {

  "run" should "report a missing size" in {
    val result = EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(new File("target/testSDO")),
      datastreamLocation = Some(new URL("http://x.nl/l/d")),
      size = None,
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = None,
      subordinate = FedoraRelationObject("easy-dataset:1"),
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    ))

    inside(result) {
      case Failure(e: NoSuchElementException) => e should have message "None.get"
    }
    // the message is vague, a proper FileItemConf/EasyStageDataset
    // should take care of something clearer for the end user
  }

  it should "report a fedora error" in {
    val result = EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(new File("target/testSDO")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = FedoraRelationObject("easy-dataset:1"),
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Failure(new Exception("mocked error"))
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    ))

    inside(result) {
      case Failure(e) => e should have message "mocked error"
    }
  }

  it should "report the dataset does not exist" in {
    val result = EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(new File("target/testSDO")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = FedoraRelationObject("easy-dataset:1"),
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq() // TODO findObjects should return a Try
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    ))

    inside(result) {
      case Failure(e) => e should have message "easy-dataset:1 does not exist in repository"
    }
  }

  "createFileSdo" should "use file name even if a title is provided" in {
    val sdoSetDir = new File("target/testSdoSet")
    val sdoDir = new File(sdoSetDir, "path_to_uuid-as-file-name")
    sdoSetDir.mkdirs()
    val s = FileItemSettings(
      sdoSetDir = sdoSetDir,
      file = Some(new File("path/to/uuid-as-file-name")),
      datastreamLocation = None,
      ownerId = "testOwner",
      pathInDataset = new File("path/to/uuid-as-file-name"),
      size = Some(1),
      format = Some("text/plain"),
      sha1 = None,
      title = Some("A nice title"),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS,
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "")
    s.foreach(EasyStageFileItem.createFileSdo(sdoDir, FedoraRelationObject("ficticiousParentSdo"))(_))

    val efmd = loadXML(new File(sdoDir, "EASY_FILE_METADATA"))
    (efmd \ "name").text shouldBe "uuid-as-file-name"
    (efmd \ "path").text shouldBe "path/to/uuid-as-file-name"
    val foxml = loadXML(new File(sdoDir, "fo.xml"))
    (foxml \ "datastream" \ "datastreamVersion" \ "xmlContent" \ "dc" \ "title").text shouldBe "A nice title"
  }

  it should "use the filename if title is not provided" in {
    val sdoSetDir = new File("target/testSdoSet")
    val sdoDir = new File(sdoSetDir, "path_to_uuid-as-file-name")
    sdoSetDir.mkdirs()
    val s = FileItemSettings(
      sdoSetDir = sdoSetDir,
      file = Some(new File("path/to/uuid-as-file-name")),
      datastreamLocation = None,
      ownerId = "testOwner",
      pathInDataset = new File("path/to/uuid-as-file-name"),
      size = Some(1),
      format = Some("text/plain"),
      sha1 = None,
      title = None,
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS,
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "")
    s.foreach(EasyStageFileItem.createFileSdo(sdoDir, FedoraRelationObject("ficticiousParentSdo"))(_))

    val efmd = loadXML(new File(sdoDir, "EASY_FILE_METADATA"))
    (efmd \ "name").text shouldBe "uuid-as-file-name"
    (efmd \ "path").text shouldBe "path/to/uuid-as-file-name"
    val foxml = loadXML(new File(sdoDir, "fo.xml"))
    (foxml \ "datastream" \ "datastreamVersion" \ "xmlContent" \ "dc" \ "title").text shouldBe "uuid-as-file-name"
  }

  def mockEasyFilesAndFolders(expectations: Map[String, Try[ExistingAncestor]]): EasyFilesAndFolders = {
    (file, datasetId) => expectations(s"$datasetId $file")
  }

  def mockFedora(expectations: Map[String, Seq[String]]): Fedora = {
    (query, _, _) => expectations(query)
  }
}
