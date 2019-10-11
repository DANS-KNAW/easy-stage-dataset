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

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.pf.language.ddm.api.Ddm2EmdCrosswalk
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.binding.EmdMarshaller
import nl.knaw.dans.pf.language.emd.types.{ BasicIdentifier, EmdArchive, EmdConstants }

import scala.util.{ Failure, Success, Try }

object EMD extends DebugEnhancedLogging {

  def create(sdoDir: File, licenseAccepted: Option[Boolean])(implicit s: Settings): Try[EasyMetadata] = {
    trace(sdoDir)
    new File(s.bagitDir, "metadata/dataset.xml") match {
      case file if file.exists() =>
        for {
          emd <- getEasyMetadata(file)
          _ = s.urn.foreach(urn => emd.getEmdIdentifier.add(wrapUrn(urn)))
          _ = s.doi.foreach(doi => emd.getEmdIdentifier.add(wrapDoi(doi, s.otherAccessDoi)))
          _ = emd.getEmdIdentifier.add(createDmoIdWithPlaceholder())
          _ = emd.getEmdOther.getEasApplicationSpecific.setArchive(createEmdArchive(s.archive))
          _ = licenseAccepted.foreach(emd.getEmdRights.setAcceptedLicense)
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

  private def getEasyMetadata(ddm: File): Try[EasyMetadata] = {
    trace(ddm)
    Try {
      val crosswalk = new Ddm2EmdCrosswalk()
      Option(crosswalk.createFrom(ddm))
        .map(Success(_))
        .getOrElse(Failure(new RuntimeException(s"${ crosswalk.getXmlErrorHandler.getMessages }")))
    }.flatten
  }

  private def wrapUrn(urn: String): BasicIdentifier = {
    trace(urn)
    new BasicIdentifier(urn) {
      setScheme(EmdConstants.SCHEME_PID)
      setIdentificationSystem(new URI("http://www.persistent-identifier.nl"))
    }
  }

  private def wrapDoi(doi: String, otherAccessDOI: Boolean): BasicIdentifier = {
    trace(doi, otherAccessDOI)
    new BasicIdentifier(doi) {
      setScheme(if (otherAccessDOI) EmdConstants.SCHEME_DOI_OTHER_ACCESS
                else EmdConstants.SCHEME_DOI)
      setIdentificationSystem(new URI(EmdConstants.DOI_RESOLVER))
    }
  }

  private def createDmoIdWithPlaceholder(): BasicIdentifier = {
    trace(())
    new BasicIdentifier("$sdo-id") {
      setScheme(EmdConstants.SCHEME_DMO_ID)
    }
  }

  private def createEmdArchive(archive: String): EmdArchive = {
    trace(archive)
    new EmdArchive() {
      setLocation(EmdArchive.Location.valueOf(archive))
    }
  }
}
