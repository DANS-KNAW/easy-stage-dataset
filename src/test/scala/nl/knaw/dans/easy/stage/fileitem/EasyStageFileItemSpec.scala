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

import java.io.{FileInputStream, File}

import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import org.apache.commons.io.IOUtils
import org.scalatest.{FlatSpec, Matchers}
import nl.knaw.dans.easy.stage.CustomMatchers._

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

  "createItems" should "create some SDOs" in {
    val args = "--dataset-id easy-dataset:1 --size 1 --format video/mpeg --path-in-dataset original/newSub/file.mpeg --datastream-location http://x.nl/l/d target/testsdo".split(" ")
    implicit val settings = FileItemSettings(args)
    val sdoSetDir = new File("target/testSDO")
    sdoSetDir.mkdirs()
    createItems("easy-folder:1","original",Seq("newSub","file.mpeg"),sdoSetDir)
    List(
      "newSub/cfg.json",
      "newSub/EASY_ITEM_CONTAINER_MD",
      "newSub/fo.xml",
      "newSub_file_mpeg/cfg.json",
      "newSub_file_mpeg/EASY_FILE_METADATA",
      "newSub_file_mpeg/fo.xml"
    ).foreach(f => {
      val actual = new File(sdoSetDir, f)
      val expected = new File("src/test/resources/expectedSDO",f)
      actual should haveSameContentAs(expected)
    })
  }
}
