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

import java.io.{ File, StringReader }
import java.nio.file.Path

import javax.xml.transform.stream.StreamSource
import org.apache.commons.io.FileUtils.deleteDirectory
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.Success
import scala.xml.PrettyPrinter

class AmdSpec extends MdFixture {
  // an indentation of zero allows simple comparison
  private val prettyPrinter: PrettyPrinter = new scala.xml.PrettyPrinter(1024, 0)

  private val triedSchema = loadSchema("https://easy.dans.knaw.nl/schemas/bag/metadata/agreements/2019/09/agreements.xsd")

  private val nowIso = DateTime.now.toString(ISODateTimeFormat.dateTime())

  "apply" should "validate for each test bag" in pendingUntilFixed {
    val depositorInfoDir: Path = sdoSetDir.toPath.resolve("metadata/depositor-info")
    // TODO add name space location to generated xml?
    assume(isAvailable(triedSchema))
    for (bag <- new File("src/test/resources/dataset-bags").listFiles()) {
      sdoSetDir.mkdirs()
      val amd = AMD("foo", DateTime.now, "SUBMITTED", DepositorInfo(depositorInfoDir))
      val validation = triedSchema.map(
        _.newValidator().validate(new StreamSource(new StringReader(prettyPrinter.format(amd))))
      )
      (bag, validation) shouldBe(bag, a[Success[_]]) // TODO use behave like (as in EmdSpec)
      deleteDirectory(sdoSetDir)
    }
  }

  it should "generate a remark" in {
    val msg1 = "this dataset DOES NOT contain Privacy Sensitive data."
    val msg2 = "Please contact me about blabla"
    val info = DepositorInfo(acceptedLicense = Some(false), privacySensitiveRemark = msg1, messageFromDepositor = msg2)
    val amd = AMD("foo", DateTime.now, "SUBMITTED", info)
    val formattedAmd = prettyPrinter.format(amd)

    formattedAmd should include(prettyPrinter.format(
       <stateChangeDates>
         <damd:stateChangeDate>
           <fromState>DRAFT</fromState>
           <toState>SUBMITTED</toState>
           <changeDate>{ nowIso }</changeDate>
         </damd:stateChangeDate>
       </stateChangeDates>
    ))

    formattedAmd should include(prettyPrinter.format(
      <remarks>
        <remark>
          <text>{ msg1 } { msg2 }</text>
          <remarkerId></remarkerId>
          <remarkDate>{ nowIso }</remarkDate>
        </remark>
      </remarks>
    ))
    // not formatted remark (as shown in the webui):
    (amd \\ "remarks" \\ "text").text shouldBe
      s"""$msg1
         |
         |$msg2
         |""".stripMargin.trim
  }

  it should "not generate state changes for a DRAFT (and an empty remark)" in {
    val info = DepositorInfo(acceptedLicense = None, privacySensitiveRemark = "", messageFromDepositor = "")
    val amd = AMD("foo", DateTime.now, "DRAFT", info)
    val formattedAmd = prettyPrinter.format(amd)

    formattedAmd should include(prettyPrinter.format(<stateChangeDates/>))
    formattedAmd should include(prettyPrinter.format(<remarks></remarks>))
  }
}
