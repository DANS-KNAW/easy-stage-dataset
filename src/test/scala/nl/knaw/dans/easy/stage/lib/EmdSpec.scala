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

import nl.knaw.dans.easy.stage.dataset.EMD
import nl.knaw.dans.easy.stage.fileitem.UserCategory
import org.apache.commons.io.FileUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.util.{Failure, Success}

class EmdSpec extends FlatSpec with Matchers {

  // Can't test the create method as it implicitly needs Settings which needs a fedora connection

  "each test bag" should "have a valid DDM file" in {
    for (bag <- FileUtils.listFiles(new File("src/test/resources/dataset-bags"),Array[String](),false).toArray){
      EMD.getEasyMetadata(new File(bag+"/metadata/dataset.xml")) shouldBe a[Success[_]]
    }
  }

  "invalid access category" should "fail to produce EMD" in {
    val ddm = <ddm:DDM xmlns:dcx="http://easy.dans.knaw.nl/schemas/dcx/"
                       xmlns:dc="http://purl.org/dc/elements/1.1/"
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
    val tmpDDM = new File("target/test/tmp/ddm.xml")
    FileUtils.write(tmpDDM,ddm.toString())
    EMD.getEasyMetadata(tmpDDM) shouldBe a[Failure[_]]
    tmpDDM.delete()
    println(UserCategory)
  }
}
