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
import java.nio.charset.StandardCharsets

import org.apache.commons.io.FileUtils

class DepositorInfoSpec extends MdFixture {

  val infoDir = new File(sdoSetDir.getParentFile + "/input/metadata/deposit-info")

  "constructor" should "handle an empty info dir" in { // as in the minimal bag
    DepositorInfo(infoDir.toPath) shouldBe
      DepositorInfo(acceptedLicense = None, privacySensitiveRemark = "", messageFromDepositor = "")
  }

  it should "handle the medium bag" in {
    fromAgreements(replacing = "", by = "") shouldBe
      DepositorInfo(
        acceptedLicense = Some(true),
        privacySensitiveRemark = "According to depositor First Namen (user001, does.not.exist@dans.knaw.nl) this dataset DOES contain Privacy Sensitive data.",
        messageFromDepositor = "Beware!!! Very personal data!!!",
      )
  }

  it should "handle invalid agreements.xml" in {
    infoDir.mkdirs()
    FileUtils.write(new File(infoDir + "/agreements.xml"), "blablabla", StandardCharsets.UTF_8)
    DepositorInfo(infoDir.toPath) shouldBe
      DepositorInfo(
        acceptedLicense = None,
        privacySensitiveRemark = "File agreements.xml not valid: Content is not allowed in prolog.",
        messageFromDepositor = "",
      )
  }

  it should "escape the content of message-from-depositor.txt" in {
    infoDir.mkdirs()
    FileUtils.write(new File(infoDir + "/message-from-depositor.txt"), "blab<br>labla", StandardCharsets.UTF_8)
    DepositorInfo(infoDir.toPath) should matchPattern {
      case DepositorInfo(_, _, "blab&lt;br&gt;labla") =>
    }
  }

  "privacySensitiveRemark" should "create a remark for SignerId without attributes" in {
    fromAgreements(replacing = """(easy-account="user001"| email="does.not.exist@dans.knaw.nl")""", by = "").privacySensitiveRemark shouldBe
      "According to depositor First Namen this dataset DOES contain Privacy Sensitive data."
  }

  it should "create a remark for SignerId without email attribute" in {
    fromAgreements(replacing = """(email="does.not.exist@dans.knaw.nl")""", by = "").privacySensitiveRemark shouldBe
      "According to depositor First Namen (user001) this dataset DOES contain Privacy Sensitive data."
  }

  it should "create a remark for SignerId without account attribute" in {
    fromAgreements(replacing = """(easy-account="user001")""", by = "").privacySensitiveRemark shouldBe
      "According to depositor First Namen (does.not.exist@dans.knaw.nl) this dataset DOES contain Privacy Sensitive data."
  }

  it should "create a remark for SignerId with neither email full name" in {
    fromAgreements(replacing = """(First Namen| email="does.not.exist@dans.knaw.nl")""", by = "").privacySensitiveRemark shouldBe
      "According to depositor user001 this dataset DOES contain Privacy Sensitive data."
  }

  it should "create a remark for SignerId without a full name" in {
    fromAgreements(replacing = "First Namen", by = "").privacySensitiveRemark shouldBe
      "According to depositor user001 (does.not.exist@dans.knaw.nl) this dataset DOES contain Privacy Sensitive data."
  }

  it should "create a remark for a dataset without privacy sensitive data" in {
    fromAgreements(replacing = "<containsPrivacySensitiveData>true", by = "<containsPrivacySensitiveData>false").privacySensitiveRemark shouldBe
      "According to depositor First Namen (user001, does.not.exist@dans.knaw.nl) this dataset DOES NOT contain Privacy Sensitive data."
  }

  it should "create a remark without a privacy claim" in {
    fromAgreements(replacing = "<containsPrivacySensitiveData>true</containsPrivacySensitiveData>", by = "").privacySensitiveRemark shouldBe
      "No statement by First Namen (user001, does.not.exist@dans.knaw.nl) could be found whether this dataset contains Privacy Sensitive data."
  }

  it should "create a remark without a signer" in {
    fromAgreements(replacing = """<signerId easy-account="user001" email="does.not.exist@dans.knaw.nl">First Namen</signerId>""", by = "").privacySensitiveRemark shouldBe
      "According to depositor NOT KNOWN this dataset DOES contain Privacy Sensitive data."
  }

  it should "create a remark without any signer values" in {
    fromAgreements(replacing = """(First Namen|easy-account="user001"| email="does.not.exist@dans.knaw.nl")""", by = "").privacySensitiveRemark shouldBe
      "According to depositor NOT KNOWN this dataset DOES contain Privacy Sensitive data."
  }

  private def fromAgreements(replacing: String, by: String) = {
    sdoSetDir.mkdirs()
    FileUtils.copyDirectory(
      new File("src/test/resources/dataset-bags/medium/metadata/depositor-info"),
      infoDir
    )
    if (replacing != "") {
      val agreementsFile = new File(infoDir + "/agreements.xml")
      FileUtils.write(
        agreementsFile,
        FileUtils.readFileToString(agreementsFile, StandardCharsets.UTF_8).replaceAll(replacing, by),
        StandardCharsets.UTF_8
      )
    }
    DepositorInfo(infoDir.toPath)
  }
}
