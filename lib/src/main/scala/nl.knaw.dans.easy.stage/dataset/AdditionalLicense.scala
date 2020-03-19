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

import java.io.{ ByteArrayInputStream, File, InputStream }
import java.net.URI
import java.nio.charset.StandardCharsets

import nl.knaw.dans.easy.stage.dataset.Util.loadBagXML
import nl.knaw.dans.easy.stage.lib.Constants
import nl.knaw.dans.easy.stage.{ RejectedDepositException, Settings }
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.FileUtils
import org.joda.time.DateTime
import resource.{ ManagedResource, Using, managed }

import scala.util.{ Failure, Success, Try }
import scala.xml.{ Node, NodeSeq }

object AdditionalLicense extends DebugEnhancedLogging {
  type MimeType = String
  type FileName = String
  private val ddmFileName = "metadata/dataset.xml"

  def createOptionally(sdo: File)(implicit s: Settings): Try[Option[(FileName, MimeType)]] = {
    trace(sdo)
    loadBagXML(ddmFileName) \\ "DDM" \ "dcmiMetadata" \ "license" match {
      case Seq() => Success(Option.empty)
      case _ => create(sdo).map(Option(_))
    }
  }

  private def create(sdo: File)(implicit s: Settings): Try[(FileName, MimeType)] = {
    for {
      ddm <- Try { loadBagXML(ddmFileName) }
      rightsHolder <- getRightsHolder(ddm)
      year <- getYear(ddm)
      (template, fileName, mimetype) <- getAdditionalLicenseTemplate(ddm, rightsHolder, year)
      _ <- copyAdditionalLicense(template, sdo)
    } yield (fileName, mimetype)
  }

  private def getAdditionalLicenseTemplate(ddm: NodeSeq, rightsHolder: String, year: String)(implicit s: Settings): Try[(ManagedResource[InputStream], FileName, MimeType)] = {
    ddm \\ "DDM" \ "dcmiMetadata" \ "license" match {
      case Seq(license) if hasXsiType(license, "http://purl.org/dc/terms/", "URI") =>
        for {
          uri <- Try { new URI(license.text) }
          licenseTemplateFile <- getMatchingLicense(uri, s.licenses).map(Success(_))
            .getOrElse(Failure(RejectedDepositException(s"Not a valid license URI: ${ license.text }")))
          name = licenseTemplateFile.getName
          (mimetype, content) <- getMimeTypeAndLicenseStream(licenseTemplateFile, rightsHolder, year)
        } yield (content, name, mimetype)
      case Seq(license) => Success(managed(new ByteArrayInputStream(license.text.getBytes(StandardCharsets.UTF_8))), "additional_license.txt", "text/plain")
      case lics => Failure(RejectedDepositException(s"Found ${ lics.size } dcterms:license elements. There should be exactly one"))
    }
  }

  /**
   * Retrieves a matching license, while being liberal in what it accepts:
   *
   * - both http and https accepted
   * - a trailing slash is ignored
   *
   * @param uri      the URI of the license
   * @param licenses the map from license URI-string to license File
   * @return the license File if found, otherwise None
   */
  private def getMatchingLicense(uri: URI, licenses: Map[String, File]): Option[File] = {
    def withScheme(uri: URI, scheme: String) = {
      new URI(scheme, uri.getUserInfo, uri.getHost, uri.getPort, uri.getPath, uri.getQuery, uri.getFragment)
    }

    val httpUri = uri match {
      case u if u.getScheme == "https" => withScheme(uri, "http")
      case u => u
    }

    licenses.get(httpUri.toASCIIString)
      .orElse {
        if (httpUri.toASCIIString.last == '/') licenses.get(httpUri.toASCIIString.init)
        else Option.empty
      }
  }

  def hasXsiType(e: Node, attributeNamespace: String, attributeValue: String): Boolean = {
    e.head.attribute("http://www.w3.org/2001/XMLSchema-instance", "type") match {
      case Some(Seq(n)) => n.text.split("\\:") match {
        case Array(pref, label) => e.head.getNamespace(pref) == attributeNamespace && label == attributeValue
        case _ => false
      }
      case _ => false
    }
  }

  private def getMimeTypeAndLicenseStream(licenseTemplateFile: File, rightsHolder: String, year: String): Try[(MimeType, ManagedResource[InputStream])] = {
    licenseTemplateFile.getName.split("\\.").last match {
      case "txt" => Try { "text/plain" -> asStream(readAndReplace(licenseTemplateFile, rightsHolder, year)) }
      case "html" => Try { "text/html" -> asStream(readAndReplace(licenseTemplateFile, rightsHolder, year)) }
      case "pdf" => Try { "application/pdf" -> Using.fileInputStream(licenseTemplateFile) }
      case ext => Failure(RejectedDepositException(s"Unknown extension for license: .$ext"))
    }
  }

  private def readAndReplace(licenseTemplateFile: File, rightsHolder: String, year: String): String = {
    FileUtils.readFileToString(licenseTemplateFile, StandardCharsets.UTF_8)
      .replace("<rightsHolder>", rightsHolder)
      .replace("<year>", year)
  }

  private def asStream(string: String): ManagedResource[InputStream] = {
    managed { new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8)) }
  }

  private def copyAdditionalLicense(template: ManagedResource[InputStream], sdo: File): Try[File] = Try {
    val additionalLicenseFile = new File(sdo, Constants.ADDITIONAL_LICENSE)

    template.map(FileUtils.copyInputStreamToFile(_, additionalLicenseFile))
      .acquireAndGet(_ => additionalLicenseFile)
  }

  private def getRightsHolder(ddm: NodeSeq): Try[String] = {
    ddm \\ "DDM" \ "dcmiMetadata" \ "rightsHolder" match {
      case Seq() => Failure(RejectedDepositException("No dcterms:rightsHolder element found. There should be at least one"))
      case rs => Try { rs.map(_.text).mkString(", ") }
    }
  }

  private def getYear(ddm: NodeSeq): Try[String] = {
    val years = ddm \\ "DDM" \ "profile" \ "created"
    years match {
      case Seq(year) => Try { DateTime.parse(year.text).getYear.toString }
      case ys => Failure(RejectedDepositException(s"${ ys.size } ddm:created elements found. There must be exactly one"))
    }
  }
}
