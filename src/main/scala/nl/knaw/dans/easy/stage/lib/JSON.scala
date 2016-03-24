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
package nl.knaw.dans.easy.stage.lib

import java.net.URL

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.fileitem.FileItemSettings
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.Try

object JSON {
  val HAS_DOI = "http://dans.knaw.nl/ontologies/relations#hasDoi"
  val HAS_PID = "http://dans.knaw.nl/ontologies/relations#hasPid"
  val HAS_MODEL = "info:fedora/fedora-system:def/model#hasModel"
  val IS_MEMBER_OF = "http://dans.knaw.nl/ontologies/relations#isMemberOf"
  val IS_SUBORDINATE_TO = "http://dans.knaw.nl/ontologies/relations#isSubordinateTo"
  val STORED_IN_DARKARCHIVE = "http://dans.knaw.nl/ontologies/relations#storedInDarkArchive"

  def createDatasetCfg(mimeType: Option[String], audiences: Seq[String])(implicit s: Settings): Try[String]= Try {
    def checkProvided(name: String, v: Option[String]) = if(v.isEmpty) throw new IllegalStateException(s"$name must be provided")
    checkProvided("DOI", s.DOI)
    checkProvided("URN", s.URN)
    checkProvided("Additional license MIME-type", mimeType)

    val datastreams =
      List(
        ("contentFile" -> "AMD") ~
        ("dsID" -> "AMD") ~
        ("label" -> "Administrative metadata for this dataset") ~
        ("controlGroup" -> "X") ~
        ("mimeType" -> "text/xml")
        ,
        ("contentFile" -> "EMD") ~
        ("dsID" -> "EMD") ~
        ("controlGroup" -> "X") ~
        ("mimeType" -> "text/xml"),

        ("contentFile" -> "PRSQL") ~
        ("dsID" -> "PRSQL") ~
        ("controlGroup" -> "X") ~
        ("mimeType" -> "text/xml")) ++ // N.B. an empty line after this will cause a compilation failure
        mimeType.toList.map(_ =>
        ("contentFile" -> "ADDITIONAL_LICENSE") ~
        ("dsID" -> "ADDITIONAL_LICENSE") ~
        ("controlGroup" -> "M") ~
        ("mimeType" -> mimeType.get))

    def sdoCfg(audiences: Seq[String]) =
      ("namespace" -> "easy-dataset") ~
      ("datastreams" -> datastreams) ~
      ("relations" -> (List(
        ("predicate" -> HAS_DOI) ~ ("object" -> s.DOI) ~ ("isLiteral" -> true),
        ("predicate" -> HAS_PID) ~ ("object" -> s.URN) ~ ("isLiteral" -> true),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-model:recursive-item-v1"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1DATASET"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:oai-item1"),
        ("predicate" -> STORED_IN_DARKARCHIVE) ~ ("object" -> "true") ~ ("isLiteral" -> true)
        ) ++ audiences.map(audience =>
          ("predicate" -> IS_MEMBER_OF) ~ ("object" -> s"info:fedora/${s.disciplines(audience)}"))
      ))

    pretty(render(sdoCfg(audiences)))
  }

  def createFileCfg(fileLocation: URL,
                    mimeType: String,
                    parent: (String,String),
                    subordinate: (String,String))(implicit settings: FileItemSettings): String = {
    def mendeleyJSON = {
      ("namespace" -> "easy-file") ~
        ("datastreams" -> List(
          ("dsLocation" -> fileLocation.toString) ~
            ("dsID" -> "EASY_FILE") ~
            ("controlGroup" -> "R") ~
            ("mimeType" -> mimeType),
          ("contentFile" -> "EASY_FILE_METADATA") ~
            ("dsID" -> "EASY_FILE_METADATA") ~
            ("controlGroup" -> "X") ~
            ("mimeType" -> "text/xml"))) ~
        ("relations" -> List(
          ("predicate" -> IS_MEMBER_OF) ~ parent,
          ("predicate" -> IS_SUBORDINATE_TO) ~ subordinate,
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")))
    }

    def multiDepositJSON = {
      ("namespace" -> "easy-file") ~
        ("datastreams" -> List(
          ("contentFile" -> "EASY_FILE") ~
            ("dsID" -> "EASY_FILE") ~
            ("controlGroup" -> "M") ~
            ("mimeType" -> mimeType),
          ("contentFile" -> "EASY_FILE_METADATA") ~
            ("dsID" -> "EASY_FILE_METADATA") ~
            ("controlGroup" -> "X") ~
            ("mimeType" -> "text/xml"))) ~
        ("relations" -> List(
          ("predicate" -> IS_MEMBER_OF) ~ parent,
          ("predicate" -> IS_SUBORDINATE_TO) ~ subordinate,
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")))
    }

    val json = settings.isMendeley
      .filter(identity) // only b == true
      .map(_ => mendeleyJSON)
      .getOrElse(multiDepositJSON)
    pretty(render(json))
  }

  def createDirCfg(parent: (String,String),
                   dataset: (String,String)): String = {
      val json = ("namespace" -> "easy-folder") ~
        ("datastreams" -> List(
          ("contentFile" -> "EASY_ITEM_CONTAINER_MD") ~
            ("dsID" -> "EASY_ITEM_CONTAINER_MD") ~
            ("controlGroup" -> "X") ~
            ("mimeType" -> "text/xml"))) ~
        ("relations" -> List(
          ("predicate" -> IS_MEMBER_OF) ~ parent,
          ("predicate" -> IS_SUBORDINATE_TO) ~ dataset,
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FOLDER"),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
        ))
    pretty(render(json))
  }
}
