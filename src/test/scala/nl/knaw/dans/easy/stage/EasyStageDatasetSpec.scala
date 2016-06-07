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
import java.util

import nl.knaw.dans.common.lang.dataset.AccessCategory._
import nl.knaw.dans.easy.stage.EasyStageDataset._
import nl.knaw.dans.easy.stage.lib.Constants.DATASET_SDO
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.deleteDirectory
import org.apache.commons.io.filefilter.{DirectoryFileFilter, FileFileFilter}
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

  it should "create file rights computed from dataset access rights" in {

    createProps()
    val bagitDir = new File("src/test/resources/dataset-bags/no-additional-license")
    val dataDir = new File(bagitDir, "data")
    val sdoSetDir = new File("target/test/sdoSet")
    val fileMetadataFile = new File(sdoSetDir, "quicksort_hs/EASY_FILE_METADATA")
    implicit val s = createSettings(bagitDir, sdoSetDir)

    createFileAndFolderSdos(dataDir, DATASET_SDO, OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe a[Success[_]]
    FileUtils.readFileToString(fileMetadataFile) should include ("<visibleTo>KNOWN</visibleTo>")
    FileUtils.readFileToString(fileMetadataFile) should include ("<accessibleTo>KNOWN</accessibleTo>")
    deleteDirectory(sdoSetDir)

    createFileAndFolderSdos(dataDir, DATASET_SDO, ANONYMOUS_ACCESS) shouldBe a[Success[_]]
    FileUtils.readFileToString(fileMetadataFile) should include ("<visibleTo>ANONYMOUS</visibleTo>")
    FileUtils.readFileToString(fileMetadataFile) should include ("<accessibleTo>ANONYMOUS</accessibleTo>")
    deleteDirectory(sdoSetDir)

    tmpProps.delete()
  }

  "run" should "create SDO sets for all available test bags" in {

    createProps()
    val sdoSetDir = new File("target/test/sdoSet")
    for (bag <- getTestBags.toArray) {
      val bagitDir = new File("src/test/resources/dataset-bags/no-additional-license")
      implicit val s = createSettings(bagitDir, sdoSetDir)

      EasyStageDataset.run(s) shouldBe a[Success[_]]
      sdoSetDir.exists() shouldBe true

      deleteDirectory(sdoSetDir)
    }
    tmpProps.delete()
  }

  def createProps() = FileUtils.write(tmpProps, "owner=dsowner\nredirect-unset-url=http://unset.dans.knaw.nl")

  def createSettings(bagitDir: File, sdoSetDir: File): Settings = {
    Settings(
      ownerId = "dpositor",
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      isMendeley = false,
      URN = Some("someUrn"),
      DOI = Some("doei"),
      disciplines = Map[String, String]("D10000" -> "easy-discipline:1")
    )
  }

  def getTestBags: util.Collection[File] = {
    val ff = new FileFileFilter {
      override def accept(pathname: File): Boolean = false
    }
    val df = new DirectoryFileFilter {
      override def accept(pathname: File): Boolean = true
    }
    FileUtils.listFilesAndDirs(new File("src/test/resources/dataset-bags/no-additional-license"), ff, df)
  }
}
