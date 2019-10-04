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
package nl.knaw.dans.easy.stage.dataset

import java.io.File

import javax.xml.transform.stream.StreamSource
import org.apache.commons.io.FileUtils.deleteDirectory
import org.joda.time.DateTime

import scala.util.Success
import scala.xml.PrettyPrinter

class AmdSpec extends MdFixture {
  private val prettyPrinter: PrettyPrinter = new scala.xml.PrettyPrinter(1024, 2)

  private val triedSchema = loadSchema("https://easy.dans.knaw.nl/schemas/bag/metadata/agreements/2019/09/agreements.xsd")

  "apply" should "validate for each test bag" in {
    assume(isAvailable(triedSchema))
    for (bag <- new File("src/test/resources/dataset-bags").listFiles()) {
      sdoSetDir.mkdirs()
      val amd =  AMD("foo", DateTime.now, "SUBMITTED",DepositorInfo(depositorInfoDir), "test-version")
      val validation = triedSchema.map(
        _.newValidator().validate(new StreamSource(prettyPrinter.format(amd)))
      )
      (bag, validation) shouldBe (bag, a[Success[_]]) // TODO use behave like (as in EmdSpec)
      deleteDirectory(sdoSetDir)
    }
  }
}
