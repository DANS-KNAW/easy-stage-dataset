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
package nl.knaw.dans.easy.stage.lib

import java.io.File

import nl.knaw.dans.easy.stage._
import nl.knaw.dans.easy.stage.dataset.EMD
import org.apache.commons.io.FileUtils.{deleteDirectory, deleteQuietly, write}
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Success

class EmdSpec extends FlatSpec with Matchers {

  val sdoSetDir = new File("target/test/EmdSpec/sdoSet")
  def newSettings(bagitDir: File): Settings = {
    new Settings(ownerId = "", bagitDir = bagitDir, sdoSetDir = sdoSetDir, disciplines = Map[String, String]())
  }

  "create" should "succeed for each test bag" in {
    assume(canConnect(xsds))

    for (bag <- new File("src/test/resources/dataset-bags").listFiles()) {
      sdoSetDir.mkdirs()
      implicit val s = newSettings(bag)
      EMD.create(sdoSetDir) shouldBe a[Success[_]]
      sdoSetDir.list() shouldBe Array("EMD")
      deleteDirectory(sdoSetDir)
    }
  }

  it should "produce an error containing possible values" in {
    assume(canConnect(xsds))

    val ddm = <ddm:DDM xmlns:dc="http://purl.org/dc/elements/1.1/"
                       xmlns:ddm="http://easy.dans.knaw.nl/schemas/md/ddm/"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
      <ddm:profile>
        <dc:title>Minimal dataset</dc:title>
        <dc:description>Minimal description</dc:description>
        <dc:creator>A Creator</dc:creator>
        <ddm:created>2015</ddm:created>
        <ddm:audience>D10000</ddm:audience>
        <ddm:accessRights>invalid</ddm:accessRights>
      </ddm:profile>
    </ddm:DDM>
    val tmpDDM = new File("target/test/EmdSpec/bag/metadata/dataset.xml")
    write(tmpDDM, ddm.toString())
    implicit val s = newSettings(tmpDDM.getParentFile.getParentFile)

    EMD.create(sdoSetDir).failed.get.getMessage should
      include("[OPEN_ACCESS, OPEN_ACCESS_FOR_REGISTERED_USERS, GROUP_ACCESS, REQUEST_PERMISSION, NO_ACCESS]")
    sdoSetDir.list() shouldBe null

    deleteQuietly(tmpDDM)
    deleteDirectory(sdoSetDir)
  }
}
