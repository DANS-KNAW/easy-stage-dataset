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

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.dataset.Util.loadBagXML
import org.apache.commons.lang.BooleanUtils

import scala.util.Try

case class AgreementData(isPersonalData: Boolean, isDepositAgreementAccepted: Boolean)

object AgreementData {
  def apply()(implicit s: Settings): AgreementData = {
    val triedAgreementData = for {
      agreementXml <- Try(loadBagXML(fileName = "metadata/agreements.xml"))
      isDepositAgreementAccepted = BooleanUtils.toBoolean((agreementXml \\ "depositAgreementAccepted").text)
      isPersonalData = BooleanUtils.toBoolean((agreementXml \\ "personalDataStatement" \ "containsPrivacySensitiveData").text)
    } yield AgreementData(isPersonalData, isDepositAgreementAccepted)
    triedAgreementData.getOrElse(AgreementData(isPersonalData = false, isDepositAgreementAccepted = false))
  }
}
