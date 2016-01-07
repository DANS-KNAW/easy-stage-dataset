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
import org.joda.time.DateTime

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.lib.Constants
import org.apache.commons.io.FileUtils

import scala.util.{Failure, Success, Try}
import scala.xml.{MetaData, Node, Elem, XML}

object AdditionalLicense {
  type MimeType = String

  def createOptionally(sdo: File)(implicit s: Settings): Try[Option[MimeType]] =
   if((getDdmXml().get \\ "DDM" \ "dcmiMetadata" \ "license").size == 0) Success(None)
   else create(sdo).map(m => Some(m))

  def create(sdo: File)(implicit s: Settings): Try[MimeType] =
    for {
      (template, mime) <- getAdditionalLicenseTemplate()
      rightsHolder <- getRightsHolder()
      year <- getYear()
      _ <- copyAdditionalLicense(template, rightsHolder, year, sdo)
    } yield mime

  def getAdditionalLicenseTemplate()(implicit s: Settings): Try[(String, MimeType)] = Try {
    val licenses = getDdmXml().get \\ "DDM" \ "dcmiMetadata" \ "license"
    licenses.toSeq match {
      case Seq(license) =>
          if(hasXsiType(license, "http://purl.org/dc/terms/", "URI")) {
            val licenseTemplateFile = s.licenses(license.text)
            (FileUtils.readFileToString(licenseTemplateFile), getLicenseMimeType(licenseTemplateFile.getName))
          }
          else (license.text, "text/plain")
      case licenses => throw new RuntimeException(s"Found ${licenses.size} dcterms:license elements. There should be exactly one")
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
      case ext => throw new IllegalArgumentException(s"Unknown extension for license: .$ext")
    }


  def getDdmXml()(implicit s: Settings): Try[Elem] = Try {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists) {
      throw new RuntimeException("Unable to find `metadata/dataset.xml` in bag.")
    }
    XML.loadFile(ddm)
  }

  def copyAdditionalLicense(template: String, rightsHolder: String, year: String, sdo: File): Try[File] = Try {
    val additionalLicenseFile = new File(sdo, Constants.ADDITIONAL_LICENSE)
    val content = template
      .replace("<rightsHolder>", rightsHolder)
      .replace("<year>", year)
    FileUtils.write(additionalLicenseFile, content)
    additionalLicenseFile
  }

  def getRightsHolder()(implicit s: Settings): Try[String] = Try {
    val rightsHolders = getDdmXml().get \\ "DDM" \ "dcmiMetadata" \ "rightsHolder"
    if(rightsHolders.size == 0) throw new RuntimeException("No dcterms:rightsHolder element found. There should be at least one")
    else rightsHolders.toList.map(_.text).mkString(", ")
  }

  def getYear()(implicit s: Settings): Try[String] = Try {
    val years = getDdmXml().get \\ "DDM" \ "profile" \ "created"
    if(years.size == 1) DateTime.parse(years.head.text).getYear.toString
    else throw new RuntimeException(s"${years.size} ddm:created elements found. There must be exactly one")
  }

}
