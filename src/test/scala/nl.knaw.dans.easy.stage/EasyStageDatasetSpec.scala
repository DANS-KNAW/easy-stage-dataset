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
package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URI
import java.nio.file.{ Files, Paths }

import nl.knaw.dans.common.lang.dataset.AccessCategory._
import nl.knaw.dans.easy.stage.EasyStageDataset._
import nl.knaw.dans.easy.stage.lib.Constants.DATASET_SDO
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.readFileToString
import org.scalatest.Inside._
import org.scalatest.{ FlatSpec, Matchers, OneInstancePerTest }

import scala.io.Source
import scala.util.{ Failure, Success }
import scala.xml.XML

class EasyStageDatasetSpec extends FlatSpec with Matchers with OneInstancePerTest {
  private val testDir = Paths.get("target/test", getClass.getSimpleName)
  FileUtils.deleteQuietly(testDir.toFile)
  Files.createDirectories(testDir)

  private val testBagMedium = Paths.get("src/test/resources/dataset-bags/medium")

  "createFileAndFolderSdos" should "stumble if the data folder does not exist" in {
    val bagDir = testDir.resolve("bag")
    val dataDir = bagDir.resolve("data") // Note that this directory is never created, on purpose.
    val sdoSetDir = testDir.resolve("someSDO")
    implicit val s: Settings = createSettings(bagDir.toFile, sdoSetDir.toFile)
    Files.createDirectories(bagDir)

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Failure[_]]
  }

  it should "be happy with an empty data folder" in {
    val bagDir = testDir.resolve("bag")
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("someSDO")
    implicit val s: Settings = createSettings(bagDir.toFile, sdoSetDir.toFile)
    Files.createDirectories(dataDir)

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    sdoSetDir.toFile shouldNot exist
  }

  it should "stumble over a manifest-sha1.txt with too many fields on a line" in {
    val bagDir = testDir.resolve("bag")
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("someSDO")
    implicit val s: Settings = createSettings(bagDir.toFile, sdoSetDir.toFile)
    Files.createDirectories(dataDir)
    FileUtils.write(bagDir.resolve("manifest-sha1.txt").toFile, "a b c")

    inside(createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS)) {
      case Failure(e) => e should have message "Invalid line in manifest-sha1.txt: a b c"
    }

    sdoSetDir.toFile shouldNot exist
  }

  it should "create file rights computed from dataset access rights by default" in {
    val bagDir = testDir.resolve("bag")
    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags/no-additional-license").toFile, bagDir.toFile)
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("SDO-set")
    val fileMetadataFile = sdoSetDir.resolve("quicksort_hs/EASY_FILE_METADATA")
    implicit val s: Settings = createSettings(bagDir.toFile, sdoSetDir.toFile)

    // Note that files.xml specifies no accessRights for data/quicksort.hs

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include("<accessibleTo>KNOWN</accessibleTo>")

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include("<accessibleTo>ANONYMOUS</accessibleTo>")
  }

  it should "override default dataset rights when rights are explicitly specified for a file" in {
    val bagDir = testDir.resolve("bag")
    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags/no-additional-license").toFile, bagDir.toFile)
    val dataDir = bagDir.resolve("data")
    val sdoSetDir = testDir.resolve("SDO-set")
    val fileMetadataFile = sdoSetDir.resolve("path_to_file_txt/EASY_FILE_METADATA")
    implicit val s: Settings = createSettings(bagDir.toFile, sdoSetDir.toFile)

    // Note that files.xml specifies NONE for data/path/to/file.txt

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include("<accessibleTo>NONE</accessibleTo>")

    createFileAndFolderSdos(dataDir.toFile, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile.toFile) should include("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile.toFile) should include("<accessibleTo>NONE</accessibleTo>")
  }

  "bag with secret file" should "have the correct visibleTo and accessibleTo keywords" in {
    val path = Paths.get(getClass.getClassLoader.getResource("dataset-bags/bag-with-secret-file").toURI)

    val result = testDir.resolve("SDO-set")
    EasyStageDataset.run(createSettings(path.toFile, result.toFile)
      .copy(licenses = Map("http://creativecommons.org/licenses/by-nc-sa/4.0/" ->
        Paths.get("src/main/assembly/dist/lic/CC-BY-NC-SA-4.0.html").toFile))
      .copy(fileUris = Map(Paths.get("data/path/to/file.txt") -> new URI("http://x")))).get

    val secretFile = result.resolve("path_to_file_txt/EASY_FILE_METADATA")
    secretFile.toFile should exist

    val xml = XML.loadFile(secretFile.toFile)
    (xml \ "visibleTo").text shouldBe "RESTRICTED_REQUEST"
    (xml \ "accessibleTo").text shouldBe "NONE"

    result.resolve("path_to_file_txt/EASY_FILE").toFile shouldNot exist

    Source.fromFile(result.resolve("path_to_file_txt/cfg.json").toFile).mkString should include
      """
        |"datastreams":[{
        |    "dsLocation":"http://x",
        |    "dsID":"EASY_FILE",
        |    "controlGroup":"R",
        |    "mimeType":"text/plain"
        |  }
      """.stripMargin
  }

  "checkFilesInBag" should "result in Success if files argument is empty" in {
    checkFilesInBag(Set.empty, testBagMedium) shouldBe a[Success[_]]
  }

  it should "result in Failure if passed list of one file that is not in the bag" in {
    inside(checkFilesInBag(Set(Paths.get("data/NOT-README.md")), testBagMedium)) {
      case Failure(RejectedDepositException(msg, _)) => msg should include("data/NOT-README.md")
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

    inside(checkFilesInBag(files, testBagMedium)) {
      case Failure(RejectedDepositException(msg, _)) =>
        msg should (include("WRONG.jpeg") and not include "image01" and not include "image02" and not include "image03")
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
      state = "DRAFT",
      archive = "EASY",
      disciplines = Map[String, String](
        "D10000" -> "easy-discipline:57",
        "D30000" -> "easy-discipline:1",
        "E10000" -> "easy-discipline:219",
        "E18000" -> "easy-discipline:226"),
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "",
      licenses = Map.empty
    )
  }
}
