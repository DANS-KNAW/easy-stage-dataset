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
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL
import java.nio.file.Paths

import nl.knaw.dans.easy.stage.ExistingAncestor
import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import nl.knaw.dans.easy.stage.fileitem.SdoFiles.readDatastreamFoxml
import nl.knaw.dans.easy.stage.lib.{ Fedora, FedoraRelationObject, SdoRelationObject }
import nl.knaw.dans.easy.stage.lib.Util.loadXML
import org.apache.commons.io.FileUtils.{ deleteQuietly, readFileToString, write }
import org.scalatest._

import scala.collection.immutable.HashMap
import scala.util.{ Failure, Success, Try }

class EasyStageFileItemSpec extends FlatSpec with Matchers with BeforeAndAfterEach {

  private val testDir = Paths.get("target/test", getClass.getSimpleName)

  override def beforeEach(): Unit = deleteQuietly(testDir.toFile)

  "getItemsToStage" should "return list of SDO with parent relations that are internally consistent" in {
    getItemsToStage(Seq("path", "to", "new", "file.txt"), new File("dataset-sdo-set"), "easy-folder:123") shouldBe
    Seq(
      (new File("dataset-sdo-set/path"), "path", FedoraRelationObject("easy-folder:123")),
      (new File("dataset-sdo-set/path_to"), "path/to", SdoRelationObject(new File("dataset-sdo-set/path"))),
      (new File("dataset-sdo-set/path_to_new"), "path/to/new", SdoRelationObject(new File("dataset-sdo-set/path_to"))),
      (new File("dataset-sdo-set/path_to_new_file_txt"), "path/to/new/file.txt", SdoRelationObject(new File("dataset-sdo-set/path_to_new")))
    )
  }

  it should "return an empty Seq when given one" in {
    getItemsToStage(Seq(), new File("dataset-sdo-set"), "easy-folder:123") shouldBe Seq()
  }

  it should "return only a file item if path contains one element" in {
    getItemsToStage(Seq("file.txt"), new File("dataset-sdo-set"), "easy-folder:123") shouldBe Seq((new File("dataset-sdo-set/file_txt"), "file.txt", FedoraRelationObject("easy-folder:123")))
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
    System.setProperty("app.home", "src/main/assembly/dist")
    val args = "src/test/resources/example.csv target/testSDO".split(" ")
    the[Exception] thrownBy EasyStageFileItem.main(args) should
      have message "no protocol: {{ easy_stage_dataset_fcrepo_service_url }}"
  }

  "run" should "report a missing size" in {
    the[NoSuchElementException] thrownBy EasyStageFileItem.run(new FileItemSettings(
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
      subordinate = FedoraRelationObject("easy-dataset:1"),
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
      subordinate = FedoraRelationObject("easy-dataset:1"),
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

  it should "not overwrite files with same names if folders don't yet exist" in {
    write(testDir.resolve("to-be-staged/data/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/some.csv").toFile,
      s"""DATASET-ID,SIZE,FORMAT,PATH-IN-DATASET,DATASTREAM-LOCATION,ACCESSIBLE-TO,VISIBLE-TO,CREATOR-ROLE,OWNER-ID,FILE-LOCATION
         |easy-dataset:1,8521,text/plain,"data/some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/data/some.txt"
         |easy-dataset:1,8585,text/plain,"some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/some.txt"""".stripMargin)

    runForEachCsvRow(mockEasyFilesAndFolders(HashMap(
      "easy-dataset:1 data/some.txt" -> Success("", "easy-folder:1"),
      "easy-dataset:1 some.txt" -> Success("", "easy-dataset:1")
    )))
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only ("some_txt", "data_some_txt", "data")
  }

  it should "not overwrite folders with same names if the parents don't yet exist" in {
    write(testDir.resolve("to-be-staged/parent/child/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/child/some.txt").toFile, "hello")
    write(testDir.resolve("to-be-staged/some.csv").toFile,
      s"""DATASET-ID,SIZE,FORMAT,PATH-IN-DATASET,DATASTREAM-LOCATION,ACCESSIBLE-TO,VISIBLE-TO,CREATOR-ROLE,OWNER-ID,FILE-LOCATION
         |easy-dataset:1,8521,text/plain,"parent/child/some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/parent/child/some.txt"
         |easy-dataset:1,8585,text/plain,"child/some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/child/some.txt"""".stripMargin)

    runForEachCsvRow(mockEasyFilesAndFolders(HashMap(
      "easy-dataset:1 parent" -> Success("", "easy-dataset:1"),
      "easy-dataset:1 parent/child" -> Success("", "easy-dataset:1"),
      "easy-dataset:1 parent/child/some.txt" -> Success("", "easy-dataset:1"),
      "easy-dataset:1 child" -> Success("", "easy-dataset:1"),
      "easy-dataset:1 child/some.txt" -> Success("", "easy-dataset:1")
    )))
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should
      contain only ("child", "child_some_txt", "parent", "parent_child", "parent_child_some_txt")

    // verifying just a sample of the generated content

    testDir.resolve("SDO/easy-dataset_1/child_some_txt").toFile.list() should contain only (
      "cfg.json",
      "fo.xml",
      "EASY_FILE",
      "EASY_FILE_METADATA"
    )
    testDir.resolve("SDO/easy-dataset_1/child").toFile.list() should contain only (
      "cfg.json",
      "fo.xml",
      "EASY_ITEM_CONTAINER_MD"
    )
    readFileToString(testDir.resolve("SDO/easy-dataset_1/child_some_txt/EASY_FILE").toFile,"UTF-8") shouldBe "hello"
    readDatastreamFoxml(testDir.resolve("SDO/easy-dataset_1/child/fo.xml").toString) should contain only(
      "dc_title" -> "child",
      "prop_state" -> "Active",
      "prop_label" -> "child",
      "prop_ownerId" -> "archie001"
    )
    readDatastreamFoxml(testDir.resolve("SDO/easy-dataset_1/child_some_txt/fo.xml").toString) should contain only(
      "dc_title" -> "some.txt",
      "prop_state" -> "Active",
      "prop_label" -> "some.txt",
      "dc_type" -> "text/plain",
      "prop_ownerId" -> "archie001"
    )
  }

  it should "not overwrite files with same names in root and sub folder" in {

    write(testDir.resolve("to-be-staged/da/ta/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/some.csv").toFile,
      s"""DATASET-ID,SIZE,FORMAT,PATH-IN-DATASET,DATASTREAM-LOCATION,ACCESSIBLE-TO,VISIBLE-TO,CREATOR-ROLE,OWNER-ID,FILE-LOCATION
         |easy-dataset:1,8521,text/plain,"da/ta/some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/da/ta/some.txt"
         |easy-dataset:1,8585,text/plain,"some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/some.txt"""".stripMargin)

    runForEachCsvRow(mockEasyFilesAndFolders(HashMap(
      "easy-dataset:1 da/ta/some.txt" -> Success("da/ta", "easy-folder:1"),
      "easy-dataset:1 some.txt" -> Success("", "easy-dataset:1")
    )))
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only ("some_txt", "da_ta_some_txt")
  }

  it should "not overwrite files with same names in sibling folders" in {

    write(testDir.resolve("to-be-staged/dir1/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/dir2/some.txt").toFile, "")
    write(testDir.resolve("to-be-staged/some.csv").toFile,
      s"""DATASET-ID,SIZE,FORMAT,PATH-IN-DATASET,DATASTREAM-LOCATION,ACCESSIBLE-TO,VISIBLE-TO,CREATOR-ROLE,OWNER-ID,FILE-LOCATION
         |easy-dataset:1,8521,text/plain,"dir1/some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/dir1/some.txt"
         |easy-dataset:1,8585,text/plain,"dir2/some.txt",,KNOWN,ANONYMOUS,ARCHIVIST,archie001,"$testDir/to-be-staged/dir2/some.txt"""".stripMargin)

    runForEachCsvRow(mockEasyFilesAndFolders(HashMap(
      "easy-dataset:1 dir1/some.txt" -> Success("dir1", "easy-folder:1"),
      "easy-dataset:1 dir2/some.txt" -> Success("dir2", "easy-folder:2")
    )))
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only ("dir1_some_txt", "dir2_some_txt")
  }

  private def runForEachCsvRow(filesAndFolders: EasyFilesAndFolders,
                               fedora: Fedora = mockFedora(HashMap(
                                 "pid~easy-dataset:1" -> Seq("easy-dataset:1")
                               )),
                               args: Array[String] = Array(
                                 "--csv-file",
                                 s"$testDir/to-be-staged/some.csv",
                                 s"$testDir/SDO"
                               )
                              ) = {
    for {
      fileItemSettings <- getSettingsRows(new FileItemConf(args)).get.map(_.copy(
        fedora = fedora,
        easyFilesAndFolders = filesAndFolders
      ))
    } EasyStageFileItem.run(fileItemSettings) shouldBe a[Success[_]]
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
    EasyStageFileItem.createFileSdo(sdoDir, FedoraRelationObject("ficticiousParentSdo"))

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
    EasyStageFileItem.createFileSdo(sdoDir, FedoraRelationObject("ficticiousParentSdo"))

    val efmd =  loadXML(new File(sdoDir, "EASY_FILE_METADATA"))
    (efmd \ "name").text shouldBe "uuid-as-file-name"
    (efmd \ "path").text shouldBe "path/to/uuid-as-file-name"
    val foxml = loadXML(new File(sdoDir, "fo.xml"))
    (foxml \ "datastream" \ "datastreamVersion" \ "xmlContent" \ "dc" \ "title").text shouldBe "uuid-as-file-name"
  }

  def mockEasyFilesAndFolders(expectations: Map[String,Try[ExistingAncestor]]): EasyFilesAndFolders =
    new EasyFilesAndFolders {
      override def getExistingAncestor(file: File, datasetId: String): Try[ExistingAncestor] =
        expectations(s"$datasetId $file")
    }

  def mockFedora(expectations: Map[String,Seq[String]]): Fedora =
    new Fedora {
      override def findObjects(query: String, acc: Seq[String], token: Option[String]): Seq[String] =
        expectations(query)
    }
}
