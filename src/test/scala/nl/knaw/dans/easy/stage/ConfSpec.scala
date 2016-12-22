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

import nl.knaw.dans.easy.stage.CustomMatchers._
import org.rogach.scallop.ScallopConf

class ConfSpec extends AbstractConfSpec {

  private def clo = new Conf(Array[String]()) {
    // avoids System.exit() in case of invalid arguments or "--help"
    override def verify(): Unit = {}
  }

  override def getConf: ScallopConf = clo

  "synopsis in help info" should "be part of README.md" in {
    new File("README.md") should containTrimmed(clo.synopsis)
  }

  "description line(s) in help info" should "be part of README.md and pom.xml" in {
    val description = clo.description
    new File("README.md") should containTrimmed(description)
    new File("pom.xml") should containTrimmed(description)
  }
}