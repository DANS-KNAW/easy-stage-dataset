/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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

import nl.knaw.dans.easy.stage.dataset.Util.loadBagXML
import nl.knaw.dans.easy.stage.lib.Constants
import nl.knaw.dans.easy.stage.{RejectedDepositException, Settings}
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime

import scala.util.{Success, Try}
import scala.xml.{Node, NodeSeq}

object AdditionalLicense {
  type MimeType = String
  private val ddmFileName = "metadata/dataset.xml"

  def createOptionally(sdo: File)(implicit s: Settings): Try[Option[MimeType]] =
   if((loadBagXML(ddmFileName) \\ "DDM" \ "dcmiMetadata" \ "license").isEmpty) Success(None)
   else create(sdo).map(m => Some(m))

  def create(sdo: File)(implicit s: Settings): Try[MimeType] =
    for {
      ddm <- Try{loadBagXML(ddmFileName)}
      (template, mime) <- getAdditionalLicenseTemplate(ddm)
      rightsHolder <- getRightsHolder(ddm)
      year <- getYear(ddm)
      _ <- copyAdditionalLicense(template, rightsHolder, year, sdo)
    } yield mime

  def getAdditionalLicenseTemplate(ddm: NodeSeq)(implicit s: Settings): Try[(String, MimeType)] = Try {
    val licenses = ddm \\ "DDM" \ "dcmiMetadata" \ "license"
    licenses match {
      case Seq(license) =>
          if(hasXsiType(license, "http://purl.org/dc/terms/", "URI")) {
            val licenseTemplateFile = getMatchingLicense(new URI(license.text), s.licenses).getOrElse(throw RejectedDepositException(s"Not a valid license URI: ${license.text}"))
            (FileUtils.readFileToString(licenseTemplateFile, "UTF-8"), getLicenseMimeType(licenseTemplateFile.getName))
          }
          else (license.text, "text/plain")
      case lics => throw RejectedDepositException(s"Found ${lics.size} dcterms:license elements. There should be exactly one")
    }
  }

  /**
   * Retrieves a matching license, while being liberal in what it accepts:
   *
   * - both http and https accepted
   * - a trailing slash is ignored
   *
   * @param uri the URI of the license
   * @param licenses the map from license URI-string to license File
   * @return the license File if found, otherwise None
   */
  def getMatchingLicense(uri: URI, licenses: Map[String, File]): Option[File] = {
    def withScheme(uri: URI, scheme: String) = new URI(scheme, uri.getUserInfo, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment)

    val httpUri =
      if (uri.getScheme == "https") withScheme(uri, "https")
      else uri

    licenses.get(httpUri.toASCIIString).orElse {
      if (httpUri.toASCIIString.last == '/') licenses.get(httpUri.toASCIIString.init)
      else None
    }
  }

  def hasXsiType(e: Node, attributeNamespace: String, attributeValue: String): Boolean =
   e.head.attribute("http://www.w3.org/2001/XMLSchema-instance", "type") match {
     case Some(Seq(n)) => n.text.split("\\:") match {
       case Array(pref, label) => e.head.getNamespace(pref) == attributeNamespace && label == attributeValue
       case _ => false
     }
     case _ => false
   }

  def getLicenseMimeType(licenseFileName: String): MimeType =
    licenseFileName.split("\\.").last match {
      case "txt" => "text/plain"
      case "html" => "text/html"
      case ext => throw RejectedDepositException(s"Unknown extension for license: .$ext")
    }

  def copyAdditionalLicense(template: String, rightsHolder: String, year: String, sdo: File): Try[File] = Try {
    val additionalLicenseFile = new File(sdo, Constants.ADDITIONAL_LICENSE)
    val content = template
      .replace("<rightsHolder>", rightsHolder)
      .replace("<year>", year)
    FileUtils.write(additionalLicenseFile, content, "UTF-8")
    additionalLicenseFile
  }

  def getRightsHolder(ddm: NodeSeq): Try[String] = Try {
    val rightsHolders = ddm \\ "DDM" \ "dcmiMetadata" \ "rightsHolder"
    if(rightsHolders.isEmpty) throw RejectedDepositException("No dcterms:rightsHolder element found. There should be at least one")
    else rightsHolders.toList.map(_.text).mkString(", ")
  }

  def getYear(ddm: NodeSeq): Try[String] = Try {
    val years = ddm \\ "DDM" \ "profile" \ "created"
    if(years.size == 1) DateTime.parse(years.head.text).getYear.toString
    else throw RejectedDepositException(s"${years.size} ddm:created elements found. There must be exactly one")
  }
}
