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

import nl.knaw.dans.easy.stage._
import nl.knaw.dans.lib.error._
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.types.{ BasicRemark, BasicString }
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FileUtils.{ deleteDirectory, deleteQuietly, write }

import scala.util.{ Failure, Success }

class EmdSpec extends MdFixture {

  "create" should "succeed for each test bag" in {
    assume(canConnect(xsds))

    for (bag <- new File("src/test/resources/dataset-bags").listFiles()) {
      sdoSetDir.mkdirs()
      implicit val s: Settings = newSettings(bag)
      (bag, EMD.create(sdoSetDir, DepositorInfo(depositorInfoDir).acceptedLicense)) should matchPattern { // TODO use behave like
        case (_, Success(_)) =>
      }
      sdoSetDir.list() shouldBe Array("EMD")
      deleteDirectory(sdoSetDir)
    }
  }

  it should "set license, containsPrivacySensitiveData and remark for the data manager" in {
    assume(canConnect(xsds))
    sdoSetDir.mkdirs()
    val mediumDir = new File("src/test/resources/dataset-bags/medium")
    implicit val s: Settings = newSettings(mediumDir)
    inside(EMD.create(sdoSetDir, DepositorInfo(depositorInfoDir).acceptedLicense)) {
      case Success(emd: EasyMetadata) =>
        val acceptBS = new BasicString("accept")
        acceptBS.setScheme("Easy2 version 1")

        emd.getEmdRights.getTermsLicense should contain only(new BasicString("http://opensource.org/licenses/MIT"), acceptBS)
        emd.getEmdOther.getEasRemarks should contain only(
          new BasicRemark("Message for the Datamanager: Beware!!! Very personal data!!!"),
          new BasicRemark("Message for the Datamanager: According to depositor First Namen (user001, does.not.exist@dans.knaw.nl) this dataset DOES contain Privacy Sensitive data.")
        )
    }
  }

  it should "create a remark for SignerId without attributes" in {
    easRemarksFromAgreements(replacing = """(easy-account="user001"| email="does.not.exist@dans.knaw.nl")""", by = "") should
      include("depositor First Namen this dataset")
  }

  it should "create a remark for SignerId without email attribute" in {
    easRemarksFromAgreements(replacing = """(email="does.not.exist@dans.knaw.nl")""", by = "") should
      include("depositor First Namen (user001) this dataset")
  }

  it should "create a remark for SignerId without account attribute" in {
    easRemarksFromAgreements(replacing = """(easy-account="user001")""", by = "") should
      include("depositor First Namen (does.not.exist@dans.knaw.nl) this dataset")
  }

  it should "create a remark for SignerId with neither email full name" in {
    easRemarksFromAgreements(replacing = """(First Namen| email="does.not.exist@dans.knaw.nl")""", by = "") should
      include("depositor user001 this dataset")
  }

  it should "create a remark for SignerId without a full name" in {
    easRemarksFromAgreements(replacing = "First Namen", by = "") should
      include("depositor user001 (does.not.exist@dans.knaw.nl) this dataset")
  }

  it should "create a remark for a dataset without privacy sensitive data" in {
    easRemarksFromAgreements(replacing = "<containsPrivacySensitiveData>true", by = "<containsPrivacySensitiveData>false") should
      include("this dataset DOES NOT contain Privacy Sensitive data")
  }

  it should "create a remark without a privacy claim" in {
    easRemarksFromAgreements(replacing = "<containsPrivacySensitiveData>true</containsPrivacySensitiveData>", by = "") should
      include("Message for the Datamanager: No statement by First Namen (user001, does.not.exist@dans.knaw.nl) could be found whether this dataset contains Privacy Sensitive data.")
  }

  it should "create a remark without a signer" in {
    easRemarksFromAgreements(replacing = """<signerId easy-account="user001" email="does.not.exist@dans.knaw.nl">First Namen</signerId>""", by = "") should
      include("Message for the Datamanager: According to depositor NOT KNOWN this dataset DOES contain Privacy Sensitive data.")
  }

  it should "create a remark without any signer values" in {
    easRemarksFromAgreements(replacing = """(First Namen|easy-account="user001"| email="does.not.exist@dans.knaw.nl")""", by = "") should
      include("Message for the Datamanager: According to depositor NOT KNOWN this dataset DOES contain Privacy Sensitive data.")
  }

  private def easRemarksFromAgreements(replacing: String, by: String) = {
    assume(canConnect(xsds))
    sdoSetDir.mkdirs()
    val mediumDir = new File("src/test/resources/dataset-bags/medium")
    val input = new File(sdoSetDir.getParentFile + "/input")
    val agreementsFile = new File(input + "/metadata/depositor-info/agreements.xml")
    FileUtils.copyDirectory(mediumDir, input)
    FileUtils.write(
      agreementsFile,
      FileUtils.readFileToString(agreementsFile).replaceAll(replacing, by)
    )
    EMD.create(sdoSetDir, DepositorInfo(depositorInfoDir).acceptedLicense)(newSettings(input))
      .getOrRecover(e => fail(e.getMessage, e))
      .getEmdOther.getEasRemarks.toString
  }

  it should "not set license, containsPrivacySensitiveData and remark for the data manager if the depositor-info dir not available" in {
    assume(canConnect(xsds))
    sdoSetDir.mkdirs()
    val minimalDir = new File("src/test/resources/dataset-bags/minimal")
    implicit val s: Settings = newSettings(minimalDir)
    inside(EMD.create(sdoSetDir, DepositorInfo(depositorInfoDir).acceptedLicense)) {
      case Success(emd: EasyMetadata) =>
        emd.getEmdRights.getTermsLicense shouldBe empty
        emd.getEmdOther.getEasRemarks shouldBe empty
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
        <ddm:available>2015</ddm:available>
        <ddm:audience>D10000</ddm:audience>
        <ddm:accessRights>invalid</ddm:accessRights>
      </ddm:profile>
    </ddm:DDM>
    val tmpDDM = new File("target/test/EmdSpec/bag/metadata/dataset.xml")
    write(tmpDDM, ddm.toString())
    implicit val s: Settings = newSettings(tmpDDM.getParentFile.getParentFile)

    inside(EMD.create(sdoSetDir, DepositorInfo(depositorInfoDir).acceptedLicense)) {
      case Failure(e) => e.getMessage should
        include("[OPEN_ACCESS, OPEN_ACCESS_FOR_REGISTERED_USERS, GROUP_ACCESS, REQUEST_PERMISSION, NO_ACCESS]")
    }

    Option(sdoSetDir.list()) shouldBe empty

    deleteQuietly(tmpDDM)
  }
}
