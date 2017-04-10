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
package nl.knaw.dans.easy.stage

import java.io.File
import java.nio.file.{Files, Paths}

import nl.knaw.dans.common.lang.dataset.AccessCategory._
import nl.knaw.dans.easy.stage.EasyStageDataset._
import nl.knaw.dans.easy.stage.lib.Constants.DATASET_SDO
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.{deleteDirectory, readFileToString}
import org.scalatest.{FlatSpec, Matchers, OneInstancePerTest}
import org.scalatest.Inside._

import scala.util.{Failure, Success}

class EasyStageDatasetSpec extends FlatSpec with Matchers with OneInstancePerTest {
  private val testDir = Paths.get("target/test", getClass.getSimpleName)
  FileUtils.deleteQuietly(testDir.toFile)
  Files.createDirectories(testDir)

  private val testBagMedium = Paths.get("src/test/resources/dataset-bags/medium")

  "createFileAndFolderSdos" should "stumble if the data folder does not exist" in {
    val bagDir = testDir.resolve("bag")
    val dataDir = bagDir.resolve("data") // Note that this directory is never created, on purpose.
    val sdoSetDir = testDir.resolve("someSDO")
    implicit val s = createSettings(bagDir.toFile, sdoSetDir.toFile)
    Files.createDirectories(bagDir)

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Failure[_]]
  }

  it should "be happy with an empty data folder" in {
    val bagDir = testDir.resolve("bag")
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("someSDO")
    implicit val s = createSettings(bagDir.toFile, sdoSetDir.toFile)
    Files.createDirectories(dataDir)

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    Files.exists(sdoSetDir) shouldBe false
  }

  it should "stumble over a manifest-sha1.txt with too many fields on a line" in {
    val bagDir = testDir.resolve("bag")
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("someSDO")
    implicit val s = createSettings(bagDir.toFile, sdoSetDir.toFile)
    Files.createDirectories(dataDir)
    FileUtils.write(bagDir.resolve("manifest-sha1.txt").toFile,"a b c")

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS).failed.get should
      have message "Invalid line in manifest-sha1.txt: a b c"
    Files.exists(sdoSetDir) shouldBe false
  }

  it should "create file rights computed from dataset access rights by default" in {
    val bagDir = testDir.resolve("bag")
    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags/no-additional-license").toFile, bagDir.toFile)
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("SDO-set")
    val fileMetadataFile = sdoSetDir.resolve("quicksort_hs/EASY_FILE_METADATA")
    implicit val s = createSettings(bagDir.toFile, sdoSetDir.toFile)

    // Note that files.xml specifies no accessRights for data/quicksort.hs

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include ("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include ("<accessibleTo>KNOWN</accessibleTo>")

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include ("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include ("<accessibleTo>ANONYMOUS</accessibleTo>")
  }

  it should "override default dataset rights when rights are explicitly specified for a file" in {
    val bagDir = testDir.resolve("bag")
    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags/no-additional-license").toFile, bagDir.toFile)
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("SDO-set")
    val fileMetadataFile = sdoSetDir.resolve("path_to_file_txt/EASY_FILE_METADATA")
    implicit val s = createSettings(bagDir.toFile, sdoSetDir.toFile)

    // Note that files.xml specifies NONE for data/path/to/file.txt

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include ("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include ("<accessibleTo>NONE</accessibleTo>")

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include ("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include ("<accessibleTo>NONE</accessibleTo>")
  }

  "checkFilesInBag" should "result in Success if files argument is empty" in {
    checkFilesInBag(Set.empty, testBagMedium) shouldBe a[Success[_]]
  }

  it should "result in Failure if passed list of one file that is not in the bag" in {
    val result = checkFilesInBag(Set(Paths.get("data/NOT-README.md")), testBagMedium)

    result shouldBe a[Failure[_]]
    inside(result) {
      case Failure(e) =>
        e shouldBe a[RejectedDepositException]
        e.getMessage should include("data/NOT-README.md")
    }
  }

  it should "result in Success if all files are found in the bag" in {
    val files = Set(
      Paths.get("data/README.md"),
      Paths.get("data/random images/image01.png"),
      Paths.get("data/random images/image02.jpeg"),
      Paths.get("data/random images/image03.jpeg"),
      Paths.get("data/a/deeper/path/With some file.txt"))

    checkFilesInBag(files, testBagMedium) shouldBe a[Success[_]]
  }

  it should "result in Failure if all files are found in the bag but an extra file is added" in {
    val files = Set(
      Paths.get("data/README.md"),
      Paths.get("data/random images/image01.png"),
      Paths.get("data/random images/image02.jpeg"),
      Paths.get("data/random images/image03.jpeg"),
      Paths.get("data/random images/WRONG.jpeg"),
      Paths.get("data/a/deeper/path/With some file.txt"))

    val result = checkFilesInBag(files, testBagMedium)
    result shouldBe a[Failure[_]]

    inside(result) {
      case Failure(e) =>
        e shouldBe a[RejectedDepositException]
        e.getMessage should include("WRONG.jpeg")
        e.getMessage shouldNot include("image01")
        e.getMessage shouldNot include("image02")
        e.getMessage shouldNot include("image03")
    }
  }

  def createSettings(bagitDir: File, sdoSetDir: File): Settings = {
    // the user and disciplines should exist in deasy
    // to allow ingest and subsequent examination with the web-ui of the generated sdo sets
    new Settings(
      ownerId = "digger001",
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      urn = Some("someUrn"),
      doi = Some("doei"),
      disciplines = Map[String, String](
        "D10000" -> "easy-discipline:57",
        "D30000" -> "easy-discipline:1",
        "E10000" -> "easy-discipline:219",
        "E18000" -> "easy-discipline:226")
    )
  }
}
