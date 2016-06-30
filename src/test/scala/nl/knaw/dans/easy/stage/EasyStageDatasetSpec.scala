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
package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.common.lang.dataset.AccessCategory._
import nl.knaw.dans.easy.Util._
import nl.knaw.dans.easy.stage.EasyStageDataset._
import nl.knaw.dans.easy.stage.lib.Constants.DATASET_SDO
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.{deleteDirectory, readFileToString}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class EasyStageDatasetSpec extends FlatSpec with Matchers {

  private val tmpProps = new File("cfg/application.properties")

  "createFileAndFolderSdos" should "stumble if the data folder does not exist" in {

    val bagitDir = new File("target/test/bag")
    val dataDir = new File(bagitDir, "data")
    val sdoSetDir = new File("target/test/someSDO")
    implicit val s = createSettings(bagitDir, sdoSetDir)
    bagitDir.mkdirs()

    createFileAndFolderSdos(dataDir, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Failure[_]]

    deleteDirectory(bagitDir)
  }

  it should "be happy with an empty data folder" in {

    val bagitDir = new File("target/test/bag")
    val dataDir = new File(bagitDir, "data")
    val sdoSetDir = new File("target/test/someSDO")
    implicit val s = createSettings(bagitDir, sdoSetDir)
    dataDir.mkdirs()

    createFileAndFolderSdos(dataDir, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    sdoSetDir.exists() shouldBe false

    deleteDirectory(bagitDir)
  }

  it should "stumble over a manifest-sha1.txt with too many fields on a line" in {

    val bagitDir = new File("target/test/bag")
    val dataDir = new File(bagitDir, "data")
    val sdoSetDir = new File("target/test/someSDO")
    implicit val s = createSettings(bagitDir, sdoSetDir)
    dataDir.mkdirs()
    FileUtils.write(new File(bagitDir,"manifest-sha1.txt"),"a b c")

    createFileAndFolderSdos(dataDir, DATASET_SDO, ANONYMOUS_ACCESS).failed.get should
      have message "Invalid line in manifest-sha1.txt: a b c"
    sdoSetDir.exists() shouldBe false

    deleteDirectory(bagitDir)
  }

  it should "create file rights computed from dataset access rights" in {

    createProps()
    val bagitDir = new File("src/test/resources/dataset-bags/no-additional-license")
    val dataDir = new File(bagitDir, "data")
    val sdoSetDir = new File("target/test/sdoSet")
    val fileMetadataFile = new File(sdoSetDir, "quicksort_hs/EASY_FILE_METADATA")
    implicit val s = createSettings(bagitDir, sdoSetDir)

    createFileAndFolderSdos(dataDir, DATASET_SDO, OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile) should include ("<visibleTo>KNOWN</visibleTo>")
    readFileToString(fileMetadataFile) should include ("<accessibleTo>KNOWN</accessibleTo>")
    deleteDirectory(sdoSetDir)

    createFileAndFolderSdos(dataDir, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    readFileToString(fileMetadataFile) should include ("<visibleTo>ANONYMOUS</visibleTo>")
    readFileToString(fileMetadataFile) should include ("<accessibleTo>ANONYMOUS</accessibleTo>")
    deleteDirectory(sdoSetDir)

    tmpProps.delete()
  }

  "run" should "create SDO sets from test bags (proof the puddings by eating them with easy-ingest)" in {
    assume(canConnect(xsds))

    createProps()

    // license-by-url seems to require mocking web-access, probably beyond the purpose of this test
    def useTestBag(f: File) = f.getName != "additional-license-by-url"

    val testBags = new File ("src/test/resources/dataset-bags").listFiles().filter(useTestBag)
    val emptyDataDir = new File("src/test/resources/dataset-bags/minimal/data")
    val puddingsDir = new File ("target/sdoPuddings")
    emptyDataDir.mkdir()

    // clean up old results
    FileUtils.deleteDirectory(puddingsDir)

    for (bag <- testBags) {
      val sdoSetDir = new File(puddingsDir,bag.getName)
      implicit val settings = createSettings(bag, sdoSetDir)

      EasyStageDataset.run(settings) shouldBe a[Success[_]]
      new File(sdoSetDir, "dataset/EMD").exists() shouldBe true
      new File(sdoSetDir, "dataset/AMD").exists() shouldBe true
      new File(sdoSetDir, "dataset/cfg.json").exists() shouldBe true
      new File(sdoSetDir, "dataset/fo.xml").exists() shouldBe true
      new File(sdoSetDir, "dataset/PRSQL").exists() shouldBe true
    }
    // a bag with an empty data folder results in a single SDO
    new File(puddingsDir,"minimal").listFiles().length shouldBe 1

    // both bags have 2 files and 2 nested folders resulting in a total of five SDO's
    new File(puddingsDir,"no-additional-license").listFiles().length shouldBe 5
    new File(puddingsDir,"additional-license-by-text").listFiles().length shouldBe 5

    // a bag with one folder with three files also result in five SDO's
    new File(puddingsDir,"one-invalid-sha1").listFiles().length shouldBe 5

    // cleanup, leave created SDO sets as puddings to proof by eating them
    emptyDataDir.delete()
    tmpProps.delete()
  }

  def createProps() = FileUtils.write(tmpProps, "owner=dsowner\nredirect-unset-url=http://unset.dans.knaw.nl")

  def createSettings(bagitDir: File, sdoSetDir: File): Settings = {
    // the user and disciplines should exist in deasy
    // to allow ingest and subsequent examination with the web-ui of the generated sdo sets
    new Settings(
      ownerId = "digger001",
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      isMendeley = false,
      URN = Some("someUrn"),
      DOI = Some("doei"),
      disciplines = Map[String, String](
        "D10000" -> "easy-discipline:57",
        "D30000" -> "easy-discipline:1",
        "E10000" -> "easy-discipline:219",
        "E18000" -> "easy-discipline:226")
    )
  }
}
