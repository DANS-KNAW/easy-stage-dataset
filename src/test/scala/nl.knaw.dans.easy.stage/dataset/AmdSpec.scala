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
import java.nio.file.Path

import nl.knaw.dans.common.jibx.JiBXObjectFactory
import nl.knaw.dans.easy.domain.dataset.AdministrativeMetadataImpl
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.deleteDirectory
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.util.{ Success, Try }
import scala.xml.PrettyPrinter

class AmdSpec extends MdFixture {
  // an indentation of zero allows simple comparison
  private val prettyPrinter: PrettyPrinter = new scala.xml.PrettyPrinter(1024, 0)
  private val nowIso = DateTime.now.toString(ISODateTimeFormat.dateTime())

  "sword2-amd" should "unMarshall" in {
    unMarshall(new File("src/test/resources/sword2-amd.xml"))
  }

  "apply" should "validate for each test bag" in {
    val depositorInfoDir: Path = sdoSetDir.toPath.resolve("metadata/depositor-info")
    //assume(isAvailable(triedSchema))
    for (bag <- new File("src/test/resources/dataset-bags").listFiles()) {
      sdoSetDir.mkdirs()
      val amd = AMD("foo", DateTime.now, "SUBMITTED", DepositorInfo(depositorInfoDir))
      // unMarshall(amd) shouldBe a[Success[_]]
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
          <remarkerId>foo</remarkerId>
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
    // unMarshall(amd) shouldBe a[Success[_]]
  }

  it should "not generate state changes for a DRAFT (and an empty remark)" in {
    val info = DepositorInfo(acceptedLicense = None, privacySensitiveRemark = "", messageFromDepositor = "")
    val amd = AMD("foo", DateTime.now, "DRAFT", info)
    val formattedAmd = prettyPrinter.format(amd)
    formattedAmd should include(prettyPrinter.format(<stateChangeDates/>))
    formattedAmd should include(prettyPrinter.format(<remarks/>))
  }

  it should "unMarshall a published AMD" in {
    // minimal differences with sword2-amd.xml
    val info = DepositorInfo(acceptedLicense = None, privacySensitiveRemark = "", messageFromDepositor = "")
    val amd = AMD("IPDBSTest", DateTime.now, "PUBLISHED", info)

    val file = new File(testDir + "/tested-amd.xml")
    FileUtils.write(file, prettyPrinter.format(amd))
    unMarshall(file) shouldBe a[Success[_]]
  }

  private def unMarshall(file: File) = {
    Try {
      JiBXObjectFactory.unmarshal( // how the webui reads it
        new AdministrativeMetadataImpl().getClass,
        FileUtils.readFileToByteArray(file)
      )
    }
  }
}
