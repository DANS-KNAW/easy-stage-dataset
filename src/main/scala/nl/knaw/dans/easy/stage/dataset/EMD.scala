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
package nl.knaw.dans.easy.stage.dataset

import java.io.File
import java.net.URI

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.pf.language.ddm.api.Ddm2EmdCrosswalk
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.binding.EmdMarshaller
import nl.knaw.dans.pf.language.emd.types.{BasicIdentifier, EmdConstants}

import scala.util.{Failure, Success, Try}

object EMD {

  def create(sdoDir: File)(implicit s: Settings): Try[EasyMetadata] = {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists()) {
      return Failure(new RuntimeException(s"Couldn't find metadata/dataset.xml"))
    }
    for {
      emd <- getEasyMetadata(ddm)
      _   = s.URN.foreach(urn => emd.getEmdIdentifier.add(wrapUrn(urn)))
      _   = s.DOI.foreach(doi => emd.getEmdIdentifier.add(wrapDoi(doi, s.otherAccessDOI)))
          /*
           * DO NOT USE getXmlString !! It will get the XML bytes and convert them to string using the
           * platform's default Charset, which may not be what we expect.
           *
           * See https://drivenbydata.atlassian.net/browse/EASY-984
           */
      _   <- writeEMD(sdoDir, new String(new EmdMarshaller(emd).getXmlByteArray, "UTF-8"))
    } yield emd
  }

  def getEasyMetadata(ddm: File): Try[EasyMetadata] =
    try {
      val crosswalk = new Ddm2EmdCrosswalk()
      val emd = crosswalk.createFrom(ddm)
      if (emd == null)
        Failure(new RuntimeException(s"${crosswalk.getXmlErrorHandler.getMessages}"))
      else
        Success(emd)
    } catch {
      case t: Throwable => Failure(t)
    }

  def wrapUrn(urn: String): BasicIdentifier = {
    val basicId = new BasicIdentifier(urn)
    basicId.setScheme(EmdConstants.SCHEME_PID)
    basicId.setIdentificationSystem(new URI("http://www.persistent-identifier.nl"))
    basicId
  }

  def wrapDoi(doi: String, otherAccessDOI: Boolean): BasicIdentifier = {
    val basicId = new BasicIdentifier(doi)
    basicId.setScheme(if (otherAccessDOI) EmdConstants.SCHEME_DOI_OTHER_ACCESS else EmdConstants.SCHEME_DOI)
    basicId.setIdentificationSystem(new URI("http://dx.doi.org"))
    basicId
  }
}
