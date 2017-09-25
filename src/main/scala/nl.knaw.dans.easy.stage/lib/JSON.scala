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
package nl.knaw.dans.easy.stage.lib

import nl.knaw.dans.easy.stage.Settings
import nl.knaw.dans.easy.stage.fileitem.FileItemSettings
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.json4s.JsonAST
import org.json4s.JsonAST.JValue
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.Try

object JSON extends DebugEnhancedLogging {
  val HAS_DOI = "http://dans.knaw.nl/ontologies/relations#hasDoi"
  val HAS_PID = "http://dans.knaw.nl/ontologies/relations#hasPid"
  val HAS_MODEL = "info:fedora/fedora-system:def/model#hasModel"
  val IS_MEMBER_OF = "http://dans.knaw.nl/ontologies/relations#isMemberOf"
  val IS_SUBORDINATE_TO = "http://dans.knaw.nl/ontologies/relations#isSubordinateTo"

  def createDatasetCfg(additionalLicenseFilenameAndMimetype: Option[(String, String)], audiences: Seq[String])(implicit s: Settings): Try[String] = Try {
    trace(additionalLicenseFilenameAndMimetype, audiences)

    checkProvided("DOI", s.doi)
    checkProvided("URN", s.urn)

    val datastreams: List[JsonAST.JObject] =
      List(
        ("contentFile" -> "AMD") ~
          ("dsID" -> "AMD") ~
          ("label" -> "Administrative metadata for this dataset") ~
          ("controlGroup" -> "X") ~
          ("mimeType" -> "text/xml"),
        ("contentFile" -> "EMD") ~
          ("dsID" -> "EMD") ~
          ("controlGroup" -> "X") ~
          ("mimeType" -> "text/xml"),

        ("contentFile" -> "PRSQL") ~
          ("dsID" -> "PRSQL") ~
          ("controlGroup" -> "X") ~
          ("mimeType" -> "text/xml")
      ) ++ additionalLicenseFilenameAndMimetype.toList.map { case (name, mimetype) =>
        ("contentFile" -> "ADDITIONAL_LICENSE") ~
          ("dsID" -> "ADDITIONAL_LICENSE") ~
          ("label" -> name) ~
          ("controlGroup" -> "M") ~
          ("mimeType" -> mimetype) }

    pretty(render(sdoCfg(audiences, datastreams)))
  }

  private def checkProvided(name: String, v: Option[String]): Unit = {
    if (v.isEmpty) throw new IllegalStateException(s"$name must be provided")
  }

  private def sdoCfg(audiences: Seq[String], datastreams: Seq[JsonAST.JObject])(implicit s: Settings): JsonAST.JObject =
    ("namespace" -> "easy-dataset") ~
      ("datastreams" -> datastreams) ~
      ("relations" -> (List(
        ("predicate" -> HAS_DOI) ~ ("object" -> s.doi) ~ ("isLiteral" -> true),
        ("predicate" -> HAS_PID) ~ ("object" -> s.urn) ~ ("isLiteral" -> true),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-model:recursive-item-v1"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1DATASET"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:oai-item1")
      ) ++ audiences.map(audience =>
        ("predicate" -> IS_MEMBER_OF) ~ ("object" -> s"info:fedora/${ s.disciplines(audience) }"))))

  def createFileCfg(mimeType: String, parent: RelationObject, subordinate: RelationObject)(implicit settings: FileItemSettings): String = {
    val json = settings.file
      .map(_ => managedFileDatastreamJson _)
      .getOrElse(redirectFileDatastreamJson _)
    (pretty _ compose render compose json.tupled).apply(mimeType, parent, subordinate)
  }

  private def createJSON(dataJSON: JValue, parent: RelationObject, subordinate: RelationObject) = {
    ("namespace" -> "easy-file") ~
      ("datastreams" -> List(
        dataJSON,
        ("contentFile" -> "EASY_FILE_METADATA") ~
          ("dsID" -> "EASY_FILE_METADATA") ~
          ("controlGroup" -> "X") ~
          ("mimeType" -> "text/xml"))) ~
      ("relations" -> List(
        ("predicate" -> IS_MEMBER_OF) ~ parent.tupled,
        ("predicate" -> IS_SUBORDINATE_TO) ~ subordinate.tupled,
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")))
  }

  private def redirectFileDatastreamJson(mimeType: String, parent: RelationObject, subordinate: RelationObject)(implicit settings: FileItemSettings) = {
    createJSON(
      ("dsLocation" -> settings.datastreamLocation.get.toURI.toASCIIString) ~
        ("dsID" -> "EASY_FILE") ~
        ("controlGroup" -> "R") ~
        ("mimeType" -> mimeType),
      parent,
      subordinate
    )
  }

  private def managedFileDatastreamJson(mimeType: String, parent: RelationObject, subordinate: RelationObject)(implicit settings: FileItemSettings) = {
    createJSON(
      ("contentFile" -> "EASY_FILE") ~
        ("dsID" -> "EASY_FILE") ~
        ("controlGroup" -> "M") ~
        ("mimeType" -> mimeType) ~
        ("checksumType" -> "SHA-1") ~
        ("checksum" -> settings.sha1),
      parent,
      subordinate
    )
  }

  def createDirCfg(parent: RelationObject, dataset: RelationObject): String = {
    val json = ("namespace" -> "easy-folder") ~
      ("datastreams" -> List(
        ("contentFile" -> "EASY_ITEM_CONTAINER_MD") ~
          ("dsID" -> "EASY_ITEM_CONTAINER_MD") ~
          ("controlGroup" -> "X") ~
          ("mimeType" -> "text/xml"))) ~
      ("relations" -> List(
        ("predicate" -> IS_MEMBER_OF) ~ parent.tupled,
        ("predicate" -> IS_SUBORDINATE_TO) ~ dataset.tupled,
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FOLDER"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    pretty(render(json))
  }
}
