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

import java.io.{ File, FileNotFoundException }
import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, NoSuchFileException, Paths }

import nl.knaw.dans.lib.error._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.lib.string._
import org.apache.commons.lang.BooleanUtils

import scala.util.Try
import scala.xml.{ Elem, XML }

case class Remarks(bagitDir: File) extends DebugEnhancedLogging {
  private val depositDir = bagitDir.getParentFile.getName
  private val depositorInfoDir = Paths.get("metadata/depositor-info")
  private val triedAgreementsXml: Try[Elem] = {
    val agreementsFile = new File(bagitDir, depositorInfoDir.resolve("agreements.xml").toString)
    Try {
      XML.loadFile(agreementsFile)
    }.doIfFailure {
      case _: FileNotFoundException => logger.warn(s"agreements.xml not found: $agreementsFile")
      case e => logger.warn(s"Could not load agreements.xml of $agreementsFile", e)
    }
  }

  def acceptedLicense: Boolean = triedAgreementsXml.map { agreementsXml =>
    (agreementsXml \\ "depositAgreementAccepted")
      .headOption
      .map { el =>
        val accepted = BooleanUtils.toBoolean(el.text)
        if (!accepted) logger.warn(s"[$depositDir] agreements.xml did NOT contain a depositAgreementAccepted")
        accepted
      }
      .getOrElse {
        logger.warn(s"[$depositDir] depositAgreementAccepted in agreements.xml was not true")
        false
      }
  }.getOrElse(false)

  def privacySensitiveRemark: String = triedAgreementsXml.map { agreementsXml =>
    val userNamePart = {
      val maybeSigner = (agreementsXml \ "depositAgreement" \ "signerId").headOption
      val maybeName = maybeSigner.map(_.text)
      val maybeAccount = maybeSigner.flatMap(node => (node \@ "easy-account").toOption)
      val maybeEmail = maybeSigner.flatMap(node => (node \@ "email").toOption)
      (maybeName, maybeAccount, maybeEmail) match {
        case (None, _, _) | (Some(""), None, None) =>
          logger.warn("The field signerId in agreements.xml could not be found or has no values")
          "NOT KNOWN"
        case (Some(name), None, None) => name
        case (Some(""), None, Some(email)) => email
        case (Some(""), Some(account), None) => account
        case (Some(""), Some(account), Some(email)) => s"$account ($email)"
        case (Some(name), None, Some(email)) => s"$name ($email)"
        case (Some(name), Some(account), None) => s"$name ($account)"
        case (Some(name), Some(account), Some(email)) => s"$name ($account, $email)"
      }
    }

    (agreementsXml \\ "containsPrivacySensitiveData")
      .headOption
      .map(node => BooleanUtils.toBoolean(node.text)) // anything but true/y[es] becomes false
      .map {
        case true => "DOES"
        case false => "DOES NOT"
      }
      .map(privacyPart => s"According to depositor $userNamePart this dataset $privacyPart contain Privacy Sensitive data.")
      .getOrElse {
        logger.warn("The field containsPrivacySensitiveData could not be found in agreements.xml")
        s"No statement by $userNamePart could be found whether this dataset contains Privacy Sensitive data."
      }
  }.getOrElse(s"No (valid) agreements.xml found in $depositDir")

  def messageFromDepositor: Option[String] = {
    val msgFromDepositor = "message-from-depositor.txt"
    Try {
      val msgForDataManager = bagitDir.toPath.resolve(depositorInfoDir).resolve(msgFromDepositor)
      new String(Files.readAllBytes(msgForDataManager), StandardCharsets.UTF_8)
    }.map { content =>
      if (content.isBlank) {
        logger.debug(msgFromDepositor + " was found but was empty, not setting a remark")
        None
      }
      else
        Some(content)
    }.getOrRecover {
      case _: NoSuchFileException =>
        logger.debug(msgFromDepositor + " not found, not setting a remark")
        None
      case e =>
        logger.error(e.getMessage, e)
        None
    }
  }
}

