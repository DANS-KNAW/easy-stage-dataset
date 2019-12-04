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
package nl.knaw.dans.easy.stage.command.fileitem

import java.nio.file.Paths

import nl.knaw.dans.easy.stage.ExistingAncestor
import nl.knaw.dans.easy.stage.command.Configuration
import nl.knaw.dans.easy.stage.command.fileitem.EasyStageFileItemCommand._
import nl.knaw.dans.easy.stage.command.fileitem.SdoFiles.readDatastreamFoxml
import nl.knaw.dans.easy.stage.fileitem.{EasyFilesAndFolders, EasyStageFileItem}
import nl.knaw.dans.easy.stage.lib.Fedora
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils.{deleteQuietly, readFileToString, write}
import org.scalatest._

import scala.collection.immutable.HashMap
import scala.util.{Success, Try}

class EasyStageFileItemCommandSpec extends FlatSpec with Matchers with Inside with BeforeAndAfterEach {

  private val testDir = Paths.get("target/test", getClass.getSimpleName)

  private val resourceDirString: String = Paths.get(getClass.getResource("/").toURI).toAbsolutePath.toString

  private val mockedConfiguration = new Configuration("version x.y.z", new PropertiesConfiguration() {
    setDelimiterParsingDisabled(true)
    load(Paths.get(resourceDirString + "/debug-config", "application.properties").toFile)
  }, Map.empty)

  // TODO copied from FileItemConfSpec. make fixture for it
  private val clo = new FileItemCommandLineOptions("-i i -d http:// -p p -s 0 --format f outdir".split(" "), mockedConfiguration)

  override def beforeEach(): Unit = deleteQuietly(testDir.toFile)

  "getSettingsRows" should "create a single row from dummy conf" in {
    // requires a FileItemConf instance with verified arguments
    inside(getSettingsRows(clo, mockedConfiguration)) {
      case Success(result) => result should have size 1
    }
  }

  it should "create multiple rows from example.csv" in {
    val args = "--csv-file src/test/resources/example.csv outdir".split(" ")
    inside(getSettingsRows(new FileItemCommandLineOptions(args, mockedConfiguration), mockedConfiguration)) {
      case Success(result) => result should have size 5
    }
  }

  "FileItemConf" should "accept correct options" in {
    val args = "--csv-file src/test/resources/file_properties-test.csv outdir".split(" ")
    val rows = getSettingsRows(new FileItemCommandLineOptions(args, mockedConfiguration), mockedConfiguration)
    inside(rows) {
      case Success(result) => result should have size 1
    }
  }

  private def runForEachCsvRow(filesAndFolders: EasyFilesAndFolders,
                               fedora: Fedora = mockFedora(HashMap("pid~easy-dataset:1" -> Seq("easy-dataset:1"))),
                               args: Array[String] = Array(
                                 "--csv-file",
                                 s"$testDir/to-be-staged/some.csv",
                                 s"$testDir/SDO"
                               )): Unit = {
    getSettingsRows(new FileItemCommandLineOptions(args, mockedConfiguration), mockedConfiguration)
      .map(_.map(_.map(_.copy(fedora = fedora, easyFilesAndFolders = filesAndFolders))))
      .map(_.foreach(_.map(EasyStageFileItem.run(_)).tried.flatten shouldBe a[Success[_]]))
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
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only("some_txt", "data_some_txt", "data")
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
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only(
      "child",
      "child_some_txt",
      "parent",
      "parent_child",
      "parent_child_some_txt"
    )

    // verifying just a sample of the generated content

    testDir.resolve("SDO/easy-dataset_1/child_some_txt").toFile.list() should contain only(
      "cfg.json",
      "fo.xml",
      "EASY_FILE",
      "EASY_FILE_METADATA"
    )
    testDir.resolve("SDO/easy-dataset_1/child").toFile.list() should contain only(
      "cfg.json",
      "fo.xml",
      "EASY_ITEM_CONTAINER_MD"
    )
    readFileToString(testDir.resolve("SDO/easy-dataset_1/child_some_txt/EASY_FILE").toFile, "UTF-8") shouldBe "hello"
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
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only("some_txt", "da_ta_some_txt")
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
    testDir.resolve("SDO/easy-dataset_1").toFile.list() should contain only("dir1_some_txt", "dir2_some_txt")
  }

  def mockEasyFilesAndFolders(expectations: Map[String, Try[ExistingAncestor]]): EasyFilesAndFolders = {
    (file, datasetId) => expectations(s"$datasetId $file")
  }

  def mockFedora(expectations: Map[String, Seq[String]]): Fedora = {
    (query, _, _) => expectations(query)
  }
}
