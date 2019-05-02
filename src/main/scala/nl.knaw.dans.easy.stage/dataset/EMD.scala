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
import java.net.URI
import java.nio.file.{ Files, Paths }

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.pf.language.ddm.api.Ddm2EmdCrosswalk
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.binding.EmdMarshaller
import nl.knaw.dans.pf.language.emd.types.{ BasicIdentifier, BasicRemark, EmdArchive, EmdConstants }
import org.apache.commons.lang.BooleanUtils

import scala.io.Source
import scala.util.{ Failure, Success, Try }
import scala.xml.XML

object EMD extends DebugEnhancedLogging {

  def create(sdoDir: File)(implicit s: Settings): Try[EasyMetadata] = {
    trace(sdoDir)
    new File(s.bagitDir, "metadata/dataset.xml") match {
      case file if file.exists() =>
        for {
          emd <- getEasyMetadata(file)
          _ = s.urn.foreach(urn => emd.getEmdIdentifier.add(wrapUrn(urn)))
          _ = s.doi.foreach(doi => emd.getEmdIdentifier.add(wrapDoi(doi, s.otherAccessDoi)))
          _ = emd.getEmdIdentifier.add(createDmoIdWithPlaceholder())
          _ = emd.getEmdOther.getEasApplicationSpecific.setArchive(createEmdArchive(s.archive))
          _ = addAgreementFields(emd)
          _ = addMessageForDataManager(emd)
          /*
           * DO NOT USE getXmlString !! It will get the XML bytes and convert them to string using the
           * platform's default Charset, which may not be what we expect.
           *
           * See https://drivenbydata.atlassian.net/browse/EASY-984
           */
          _ <- writeEMD(sdoDir, new String(new EmdMarshaller(emd).getXmlByteArray, "UTF-8"))
        } yield emd
      case _ => Failure(new RuntimeException(s"Couldn't find metadata/dataset.xml"))
    }
  }

  def addAgreementFields(emd: EasyMetadata)(implicit s: Settings): Unit = {
    val agreementPath = s.bagitDir.toPath.resolve(Paths.get("metadata/agreements.xml"))
    if (Files.exists(agreementPath)) {
      val agreementsXml = XML.loadFile(agreementPath.toFile)
      if (BooleanUtils.toBoolean((agreementsXml \\ "depositAgreementAccepted").text)) {
        emd.getEmdRights.setAcceptedLicense(true)
      }
      if (BooleanUtils.toBoolean((agreementsXml \\ "containsPrivacySensitiveData").text)) {
        emd.getEmdOther.getEasRemarks.add(new BasicRemark("containsPrivacySensitiveData"))
      }
    }
    else {
      logger.info("agreements.xml not found, not setting agreement data")
    }
  }

  private def addMessageForDataManager(emd: EasyMetadata)(implicit s: Settings): Unit = {
    val msgForDataManager = new File(s.bagitDir, "metadata/message-for-the-datamanager.txt")
    if (msgForDataManager.exists) {
      val content = Source.fromFile(msgForDataManager).mkString
      emd.getEmdOther.getEasRemarks.add(new BasicRemark(content))
    }
    else {
      logger.info("message-for-the-datamanager.txt not found, not setting a remark")
    }
  }

  def getEasyMetadata(ddm: File): Try[EasyMetadata] = {
    trace(ddm)
    Try {
      val crosswalk = new Ddm2EmdCrosswalk()
      Option(crosswalk.createFrom(ddm))
        .map(Success(_))
        .getOrElse(Failure(new RuntimeException(s"${ crosswalk.getXmlErrorHandler.getMessages }")))
    }.flatten
  }

  def wrapUrn(urn: String): BasicIdentifier = {
    trace(urn)
    new BasicIdentifier(urn) {
      setScheme(EmdConstants.SCHEME_PID)
      setIdentificationSystem(new URI("http://www.persistent-identifier.nl"))
    }
  }

  def wrapDoi(doi: String, otherAccessDOI: Boolean): BasicIdentifier = {
    trace(doi, otherAccessDOI)
    new BasicIdentifier(doi) {
      setScheme(if (otherAccessDOI) EmdConstants.SCHEME_DOI_OTHER_ACCESS
                else EmdConstants.SCHEME_DOI)
      setIdentificationSystem(new URI(EmdConstants.DOI_RESOLVER))
    }
  }

  def createDmoIdWithPlaceholder(): BasicIdentifier = {
    trace(())
    new BasicIdentifier("$sdo-id") {
      setScheme(EmdConstants.SCHEME_DMO_ID)
    }
  }

  def createEmdArchive(archive: String): EmdArchive = {
    trace(archive)
    new EmdArchive() {
      setLocation(EmdArchive.Location.valueOf(archive))
    }
  }
}
