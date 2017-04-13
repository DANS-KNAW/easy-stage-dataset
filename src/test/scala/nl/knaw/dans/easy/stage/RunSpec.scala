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
import java.nio.file.{Files, Path, Paths}

import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._
import scala.util.Success

class RunSpec extends FlatSpec with Matchers {
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
    FileUtils.copyDirectory(Paths.get("src/test/resources/dataset-bags").toFile, datasetBags.toFile)
    Files.createDirectory(datasetBags.resolve("minimal/data"))
    val puddingsDir = testDir.resolve("sdoPuddings")

    val testBags = resource.managed(Files.list(datasetBags)).acquireAndGet(_.iterator.asScala.toList)

    for (bag <- testBags) {
      val sdoSetDir = puddingsDir.resolve(bag.getFileName)
      implicit val settings = createSettings(bag.toFile, sdoSetDir.toFile)

      EasyStageDataset.run(settings) shouldBe a[Success[_]]
      sdoSetDir.resolve("dataset/EMD").toFile should exist
      sdoSetDir.resolve("dataset/AMD").toFile should exist
      sdoSetDir.resolve("dataset/cfg.json").toFile should exist
      sdoSetDir.resolve("dataset/fo.xml").toFile should exist
      sdoSetDir.resolve("dataset/PRSQL").toFile should exist
    }

    numberOfFilesInDir(puddingsDir.resolve("minimal")) shouldBe 1
    numberOfFilesInDir(puddingsDir.resolve("no-additional-license")) shouldBe 5
    numberOfFilesInDir(puddingsDir.resolve("additional-license-by-text")) shouldBe 5
    numberOfFilesInDir(puddingsDir.resolve("one-invalid-sha1")) shouldBe 5
    FileUtils.readFileToString(puddingsDir.resolve("one-invalid-sha1/dataset/EMD").toFile, "UTF-8") should include("planetoïde")
  }

  private def numberOfFilesInDir(dir: Path): Int = {
    resource.managed(Files.list(dir)).acquireAndGet(_.iterator.asScala.size)
  }

  def createSettings(bagitDir: File, sdoSetDir: File): Settings = {
    /*
     * Not the ideal solution, but we need to get the licenses list for this test to work, and it is
     * available in the dist directory.
     */
    System.setProperty("app.home", "src/main/assembly/dist")

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
        "E18000" -> "easy-discipline:226",
        "D16300" -> "easy-discipline:12435") // TODO: probably not the value in the actual deasy environment
    )
  }
}
