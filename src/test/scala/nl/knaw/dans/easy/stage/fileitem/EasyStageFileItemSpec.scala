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

import nl.knaw.dans.easy.stage.CustomMatchers._
import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import nl.knaw.dans.easy.stage.lib.Fedora
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable.HashMap
import scala.reflect.io.Path
import scala.util.{Failure, Success, Try}

class EasyStageFileItemSpec extends FlatSpec with Matchers {
  System.setProperty("app.home", "src/main/assembly/dist")
  def file(p: String) = new File(p)

  "getItemsToStage" should "return list of SDO with parent relations that are internally consistent" in {
    getItemsToStage(Seq("path", "to", "new", "file.txt"), file("dataset-sdo-set"), "easy-folder:123") shouldBe
    Seq((file("dataset-sdo-set/path"), "path", "object" -> "info:fedora/easy-folder:123"),
      (file("dataset-sdo-set/path_to"), "path/to", "objectSDO" -> "path"),
      (file("dataset-sdo-set/path_to_new"), "path/to/new", "objectSDO" -> "path_to"),
      (file("dataset-sdo-set/path_to_new_file_txt"), "path/to/new/file.txt", "objectSDO" -> "path_to_new"))
  }

  it should "return an empty Seq when given one" in {
    getItemsToStage(Seq(), file("dataset-sdo-set"), "easy-folder:123") shouldBe Seq()
  }

  it should "return only a file item if path contains one element" in {
    getItemsToStage(Seq("file.txt"), file("dataset-sdo-set"), "easy-folder:123") shouldBe Seq((file("dataset-sdo-set/file_txt"), "file.txt", "object" -> "info:fedora/easy-folder:123"))
  }

  "getSettingsRows" should "create a single row from dummy conf" in {
    getSettingsRows(FileItemConf.dummy).get.size shouldBe 1
  }

  it should "create multiple rows from example.csv" in {
    val args = "src/test/resources/example.csv outdir".split(" ")
    getSettingsRows(new FileItemConf(args)).get.size shouldBe 5
  }

  "main" should "report a configuration problem" in {
    val args = "src/test/resources/example.csv target/testSDO".split(" ")
    the[Exception] thrownBy EasyStageFileItem.main(args) should
      have message "no protocol: {{ easy_stage_dataset_fcrepo_service_url }}"
  }

  "run" should "create expected file item SDOs" in {
    EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(file("target/testSDO")),
      datastreamLocation = Some(new URL("http://x.nl/l/d")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(file("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      ))
    ))
    //TODO check might suffer from insignificant changes as white space or irrelevant order
    shouldBeEqual(
      Path("target/testSDO/easy-dataset_1"),
      Path("src/test/resources/expectedFileItemSDOs")
    )
    Path("target/testSDO").deleteRecursively()
  }

  it should "report a missing size" in {
    the[NoSuchElementException] thrownBy EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(file("target/testSDO")),
      datastreamLocation = Some(new URL("http://x.nl/l/d")),
      size = None,
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(file("original/newSub/file.mpeg")),
      format = None,
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      ))
    )).get should have message "None.get"
    // the message is vague, a proper FileItemConf/EasyStageDataset
    // should take care of something clearer for the end user
  }

  it should "report a fedora error" in {
    the[Exception] thrownBy EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(file("target/testSDO")),
      datastreamLocation = Some(new URL("http://x.nl/l/d")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(file("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Failure(new Exception("mocked error"))
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq("easy-dataset:1")
      ))
    )).get should have message "mocked error"
  }

  it should "report the dataset does not exist" in {
    the[Exception] thrownBy EasyStageFileItem.run(new FileItemSettings(
      sdoSetDir = Some(file("target/testSDO")),
      datastreamLocation = Some(new URL("http://x.nl/l/d")),
      size = Some(1),
      datasetId = Some("easy-dataset:1"),
      pathInDataset = Some(file("original/newSub/file.mpeg")),
      format = Some("video/mpeg"),
      subordinate = "object" -> s"info:fedora/easy-dataset:1",
      easyFilesAndFolders = mockEasyFilesAndFolders(HashMap(
        "easy-dataset:1 original/newSub/file.mpeg" -> Success("original", "easy-folder:1")
      )),
      fedora = mockFedora(HashMap(
        "pid~easy-dataset:1" -> Seq() // TODO findObjects should return a Try
      ))
    )).get should have message "easy-dataset:1 does not exist in repository"
  }

  def shouldBeEqual(actualPath: Path, expectedPath: Path): Unit = {
    // file names
    getRelativeFiles(actualPath).toSet shouldBe getRelativeFiles(expectedPath).toSet
    // content of the files
    actualPath.walk.toSeq.zip(
      expectedPath.walk.toSeq
    ).foreach {
      case (actual, expected) =>
        file(actual.toString) should haveSameContentAs(file(expected.toString))
    }
  }

  def getRelativeFiles(path: Path): List[String] =
    path.walk.map(_.toString.replaceAll(path.toString()+"/", "")).toList


  def mockEasyFilesAndFolders(expectations: Map[String,Try[(String,String)]]): EasyFilesAndFolders =
    new EasyFilesAndFolders {
      override def getExistingAncestor(file: File, datasetId: String): Try[(String, String)] =
        expectations.get(s"$datasetId $file").get
    }

  def mockFedora(expectations: Map[String,Seq[String]]): Fedora =
    new Fedora {
      override def findObjects(query: String, acc: Seq[String], token: Option[String]): Seq[String] =
        expectations.get(query).get
    }
}
