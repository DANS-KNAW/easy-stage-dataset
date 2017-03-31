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
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import nl.knaw.dans.easy.stage.fileitem.SdoFiles._
import nl.knaw.dans.easy.stage.lib.Fedora
import nl.knaw.dans.easy.stage.lib.Util.loadXML
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.HashMap
import scala.reflect.io.Path
import scala.util.{Failure, Success, Try}

class EasyStageFileItemSpec extends FlatSpec with Matchers {
  System.setProperty("app.home", "src/main/assembly/dist")

  "getItemsToStage" should "return list of SDO with parent relations that are internally consistent" in {
    getItemsToStage(Seq("path", "to", "new", "file.txt"), new File("dataset-sdo-set"), "easy-folder:123") shouldBe
    Seq((new File("dataset-sdo-set/path"), "path", "object" -> "info:fedora/easy-folder:123"),
      (new File("dataset-sdo-set/path_to"), "path/to", "objectSDO" -> "path"),
      (new File("dataset-sdo-set/path_to_new"), "path/to/new", "objectSDO" -> "path_to"),
      (new File("dataset-sdo-set/path_to_new_file_txt"), "path/to/new/file.txt", "objectSDO" -> "path_to_new"))
  }

  it should "return an empty Seq when given one" in {
    getItemsToStage(Seq(), new File("dataset-sdo-set"), "easy-folder:123") shouldBe Seq()
  }

  it should "return only a file item if path contains one element" in {
    getItemsToStage(Seq("file.txt"), new File("dataset-sdo-set"), "easy-folder:123") shouldBe Seq((new File("dataset-sdo-set/file_txt"), "file.txt", "object" -> "info:fedora/easy-folder:123"))
  }

  "getSettingsRows" should "create a single row from dummy conf" in {
    // requires a FileItemConf instance with verified arguments
    getSettingsRows(FileItemConf.dummy).get.size shouldBe 1
  }

  it should "create multiple rows from example.csv" in {
    val args = "--csv-file src/test/resources/example.csv outdir".split(" ")
    val rows = getSettingsRows(new FileItemConf(args)).get
    rows.size shouldBe 5
  }

  "FileItemConf" should "accept correct options" in {
    val args = "--csv-file src/test/resources/file_properties-test.csv outdir".split(" ")
    val rows = getSettingsRows(new FileItemConf(args)).get
    rows.size shouldBe 1
  }

  "main" should "report a configuration problem" in {
    val args = "src/test/resources/example.csv target/testSDO".split(" ")
    the[Exception] thrownBy EasyStageFileItem.main(args) should
      have message "no protocol: {{ easy_stage_dataset_fcrepo_service_url }}"
  }


  it should "create expected file item SDOs in the multi-deposit use case (i.e. when file-location is provided)" in {
    EasyStageFileItem.run(new FileItemSettings(
      ownerId = Some("testOwner"),
      sdoSetDir = Some(new File("target/testSDO")),
      file = Some(new File("original/newSub/file.mpeg")), // TODO this may fail!
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    ))

    // comparing with sample output

    // TODO sdoDir "newSub" should have been "original_newSub" to avoid potential conflicts !!!
    val actualSdoSet = Path("target/testSDO/easy-dataset_1")
    val expectedSdoSet = Path("src/test/resources/expectedFileItemSDOsWithMultiDeposit")
    getRelativeFiles(actualSdoSet) shouldBe getRelativeFiles(expectedSdoSet)
    actualSdoSet.walk.toSeq.map(_.path).sortBy(s => s).zip(
      expectedSdoSet.walk.toSeq.map(_.path).sortBy(s => s)
    ).foreach {
      case (actual, expected) if actual.endsWith("cfg.json") =>
        readCfgJson(expected) shouldBe readCfgJson(actual)
      case (actual, expected) if actual.endsWith("fo.xml") =>
        readDatastreamFoxml(actual) shouldBe readDatastreamFoxml(expected)
      case (actual, expected) => // metadata of a file or folder
        readFlatXml(actual) shouldBe readFlatXml(expected)
    }

    // a less verbose check reworded

    readDatastreamFoxml("target/testSDO/easy-dataset_1/newSub/fo.xml") shouldBe Set(
      "dc_title" -> "original/newSub",
      "prop_state" -> "Active",
      "prop_label" -> "original/newSub",
      "prop_ownerId" -> "testOwner")

    // clean up

    Path("target/testSDO").deleteRecursively()
  }

  it should "report a missing size" in {
    the[NoSuchElementException] thrownBy EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(new File("target/testSDO")),
      datastreamLocation = Some(new URL("http://x.nl/l/d")),
      size = None,
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = None,
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    )).get should have message "None.get"
    // the message is vague, a proper FileItemConf/EasyStageDataset
    // should take care of something clearer for the end user
  }

  it should "report a fedora error" in {
    the[Exception] thrownBy EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(new File("target/testSDO")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Failure(new Exception("mocked error"))
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    )).get should have message "mocked error"
  }

  it should "report the dataset does not exist" in {
    the[Exception] thrownBy EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(new File("target/testSDO")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(new File("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq() // TODO findObjects should return a Try
      )),
      accessibleTo = FileAccessRights.NONE,
      visibleTo = FileAccessRights.ANONYMOUS
    )).get should have message "easy-dataset:1 does not exist in repository"
  }

  "createFileSdo" should "use title (if exactly one provided) instead of file name" in {
    val sdoSetDir = new File("target/testSdoSet")
    val sdoDir = new File(sdoSetDir, "path_to_uuid-as-file-name")
    sdoSetDir.mkdirs()
    implicit val s = FileItemSettings(
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
      visibleTo = FileAccessRights.ANONYMOUS)
    EasyStageFileItem.createFileSdo(sdoDir, "objectSDO" -> "ficticiousParentSdo")

    val efmd =  loadXML(new File(sdoDir, "EASY_FILE_METADATA"))
    (efmd \ "name").text shouldBe "A nice title"
    (efmd \ "path").text shouldBe "path/to/A nice title"
    val foxml = loadXML(new File(sdoDir, "fo.xml"))
    (foxml \ "datastream" \ "datastreamVersion" \ "xmlContent" \ "dc" \ "title").text shouldBe "A nice title"
  }

  it should "use the filename if title is not provided" in {
    val sdoSetDir = new File("target/testSdoSet")
    val sdoDir = new File(sdoSetDir, "path_to_uuid-as-file-name")
    sdoSetDir.mkdirs()
    implicit val s = FileItemSettings(
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
      visibleTo = FileAccessRights.ANONYMOUS)
    EasyStageFileItem.createFileSdo(sdoDir, "objectSDO" -> "ficticiousParentSdo")

    val efmd =  loadXML(new File(sdoDir, "EASY_FILE_METADATA"))
    (efmd \ "name").text shouldBe "uuid-as-file-name"
    (efmd \ "path").text shouldBe "path/to/uuid-as-file-name"
    val foxml = loadXML(new File(sdoDir, "fo.xml"))
    (foxml \ "datastream" \ "datastreamVersion" \ "xmlContent" \ "dc" \ "title").text shouldBe "uuid-as-file-name"
  }

  def mockEasyFilesAndFolders(expectations: Map[String,Try[(String,String)]]): EasyFilesAndFolders =
    new EasyFilesAndFolders {
      override def getExistingAncestor(file: File, datasetId: String): Try[(String, String)] =
        expectations(s"$datasetId $file")
    }

  def mockFedora(expectations: Map[String,Seq[String]]): Fedora =
    new Fedora {
      override def findObjects(query: String, acc: Seq[String], token: Option[String]): Seq[String] =
        expectations(query)
    }
}
