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

import java.io.{ByteArrayInputStream, FileInputStream}

import nl.knaw.dans.easy.stage.fileitem.{FileItemSettings, FileItemConf}
import org.scalatest.{FlatSpec, Matchers}

class CsvSpec extends FlatSpec with Matchers {

  private val commandLineArgs = "target/test/sdo-set".split(" ")
  private val conf = new FileItemConf(commandLineArgs)

  "apply" should "fail with too few headers in the input" in {
    val in = new ByteArrayInputStream (
      "FORMAT,DATASET-ID,xxx,STAGED-DIGITAL-OBJECT-SET"
        .stripMargin.getBytes)
    the[Exception] thrownBy CSV(in, conf.longOptionNames).get should
      have message "Missing columns: PATH-IN-DATASET, PATH-IN-STORAGE"
  }

  it should "fail with uppercase in any of the required headers" in {
    val in = new ByteArrayInputStream ("".stripMargin.getBytes)
    (the[Exception] thrownBy CSV(in, Seq("ABc", "def")).get).getMessage should include ("not supported")
  }
}
