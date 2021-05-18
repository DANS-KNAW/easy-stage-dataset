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
import java.nio.file.{ Files, Path, Paths }

import nl.knaw.dans.easy.stage.lib.Constants.DATASET_SDO
import org.apache.commons.configuration.PropertiesConfiguration
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import resource._

import scala.collection.JavaConverters._
import scala.util.Success

class RunSpec extends AnyFlatSpec with Matchers with CanConnectFixture {
  private val testDir = Paths.get("target/test", getClass.getSimpleName)
  FileUtils.deleteQuietly(testDir.toFile)
  Files.createDirectories(testDir)

  /*
   * Problems with this test:
   *
   * - It depends on what is in src/test/resources/dataset-bags. If one of the bags changes or bags are added
   *   or removed, this test may unexpectedly break.
   * - It runs a very high-level function (almost `main` itself) and there depends on the
   *   complete application context (settings, licenses, etc).
   */

  "EasyStageDataset.run" should "create SDO sets from test bags (proof the puddings by eating them with easy-ingest)" in {
    assume(canConnect(xsds))
    val datasetBags = testDir.resolve("dataset-bags")
    val puddingsDir = testDir.resolve("sdoPuddings") // to be eaten manually with easy-ingest

    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags").toFile, datasetBags.toFile)
    val noDoiVariant = createNoDoiVariant(datasetBags)
    // git doesn't preserve the required empty directory:
    Files.createDirectory(datasetBags.resolve("minimal/data"))

    val testBags = resource.managed(Files.list(datasetBags)).acquireAndGet(_.iterator.asScala.toList)

    for (bag <- testBags) {
      val sdoSetDir = puddingsDir.resolve(bag.getFileName)
      implicit val settings: Settings = sdoSetDir.getFileName.toString match {
        case `noDoiVariant` => createSettings(bag.toFile, sdoSetDir.toFile, None)
        case _ => createSettings(bag.toFile, sdoSetDir.toFile, Some("doei"))
      }

      val res = EasyStageDataset.run(settings)
      res.recover { case e => e.printStackTrace() }
      res shouldBe a[Success[_]]
      sdoSetDir.resolve(s"$DATASET_SDO/EMD").toFile should exist
      sdoSetDir.resolve(s"$DATASET_SDO/AMD").toFile should exist
      sdoSetDir.resolve(s"$DATASET_SDO/dataset.xml").toFile should exist
      sdoSetDir.resolve(s"$DATASET_SDO/files.xml").toFile should exist
      sdoSetDir.resolve(s"$DATASET_SDO/cfg.json").toFile should exist
      sdoSetDir.resolve(s"$DATASET_SDO/fo.xml").toFile should exist
      sdoSetDir.resolve(s"$DATASET_SDO/PRSQL").toFile should exist
    }

    puddingsDir.resolve(s"medium/$DATASET_SDO/manifest-sha1.txt").toFile should exist
    puddingsDir.resolve(s"medium/$DATASET_SDO/agreements.xml").toFile should exist
    puddingsDir.resolve(s"medium/$DATASET_SDO/message-from-depositor.txt").toFile should exist

    numberOfFilesInDir(puddingsDir.resolve("minimal")) shouldBe 1
    numberOfFilesInDir(puddingsDir.resolve("no-additional-license")) shouldBe 5
    numberOfFilesInDir(puddingsDir.resolve("additional-license-by-text")) shouldBe 5
    numberOfFilesInDir(puddingsDir.resolve("one-invalid-sha1")) shouldBe 5
    FileUtils.readFileToString(puddingsDir.resolve(s"one-invalid-sha1/$DATASET_SDO/EMD").toFile, "UTF-8") should include("planetoïde")

    puddingsDir.resolve("medium").toFile.list() should have size 10 // DATASET_SDO + file-SDOs
    readDDM(puddingsDir, "medium") should include("doi")
    puddingsDir.resolve(noDoiVariant).toFile.list() shouldBe Array(DATASET_SDO) // no file-SDOs
    readDDM(puddingsDir, noDoiVariant) should not include "doi"
  }

  private def readDDM(puddingsDir: Path, bagName: String) = {
    FileUtils.readFileToString(puddingsDir.resolve(s"$bagName/$DATASET_SDO/dataset.xml").toFile, "UTF-8")
  }

  private def createNoDoiVariant(datasetBags: Path) = {
    val noDoiVariant = "medium-no-doi"
    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags/medium").toFile, datasetBags.resolve(noDoiVariant).toFile)
    val ddmFile = datasetBags.resolve(noDoiVariant + "/metadata/dataset.xml").toFile
    val ddmContent = FileUtils.readLines(ddmFile, "UTF-8").asScala
      .filterNot(_.contains("id-type:DOI"))
      .mkString("\n")
    FileUtils.write(ddmFile, ddmContent, "UTF-8")
    // TODO fix manifest to allow easy-ingest to eat the pudding
    noDoiVariant
  }

  private def numberOfFilesInDir(dir: Path): Int = {
    managed(Files.list(dir)).acquireAndGet(_.iterator.asScala.size)
  }

  def createSettings(bagitDir: File, sdoSetDir: File, maybeDoi: Option[String]): Settings = {
    /*
     * Not the ideal solution, but we need to get the licenses list for this test to work, and it is
     * available in the target/easy-licenses directory.
     */
    val licensesDir = Paths.get("target/easy-licenses/licenses")
    val licenses = new PropertiesConfiguration(licensesDir.resolve("licenses.properties").toFile)
    val licensesMap = licenses.getKeys.asScala.map(key => key -> licensesDir.resolve(licenses.getString(key)).toFile).toMap

    // the user and disciplines should exist in deasy
    // to allow ingest and subsequent examination with the web-ui of the generated sdo sets
    new Settings(
      ownerId = "digger001",
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      urn = Some("someUrn"),
      doi = maybeDoi,
      state = "DRAFT",
      archive = "EASY",
      disciplines = Map[String, String](
        "D10000" -> "easy-discipline:57",
        "D30000" -> "easy-discipline:1",
        "E10000" -> "easy-discipline:219",
        "E18000" -> "easy-discipline:226",
        "D16300" -> "easy-discipline:12435"),
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "", // TODO: probably not the value in the actual deasy environment
      licenses = licensesMap,
      includeBagMetadata = true,
      skipPayload = false,
    )
  }
}
