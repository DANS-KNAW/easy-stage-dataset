package nl.knaw.dans.easy.stage.lib

import java.io.File

import Constants._
import nl.knaw.dans.easy.stage.{dataset, Settings}

import dataset.Util.readAudiences // TODO fix package loop for createDatasetCfg
import nl.knaw.dans.easy.stage.lib.Util._
import org.json4s.JsonAST.JObject
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.Try

object JSON {
  val HAS_DOI = "http://dans.knaw.nl/ontologies/relations#hasDoi"
  val HAS_PID = "http://dans.knaw.nl/ontologies/relations#hasPid"
  val HAS_MODEL = "info:fedora/fedora-system:def/model#hasModel"
  val IS_MEMBER_OF = "http://dans.knaw.nl/ontologies/relations#isMemberOf"
  val IS_SUBORDINATE_TO = "http://dans.knaw.nl/ontologies/relations#isSubordinateTo"
  val DATASET_ARCHIVAL_STORAGE_LOCATION = "http://dans.knaw.nl/ontologies/relations#datasetArchivalStorageLocation"

  def createDatasetCfg(sdoDir: File, license: Option[File])(implicit s: Settings): Try[Unit] = {
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
        ("mimeType" -> "text/xml")
        ,
        ("contentFile" -> "PRSQL") ~
        ("dsID" -> "PRSQL") ~
        ("controlGroup" -> "X") ~
        ("mimeType" -> "text/xml")) ++
      license.toList.map(f =>
        ("contentFile" -> "ADDITIONAL_LICENSE") ~
        ("dsID" -> "ADDITIONAL_LICENSE") ~
        ("controlGroup" -> "M") ~
        ("mimeType" -> "text/plain"))

    def sdoCfg(audiences: Seq[String]) =
      ("namespace" -> "easy-dataset") ~
      ("datastreams" -> datastreams) ~
      ("relations" -> (List(
        ("predicate" -> HAS_DOI) ~ ("object" -> s.DOI) ~ ("isLiteral" -> true),
        ("predicate" -> HAS_PID) ~ ("object" -> s.URN) ~ ("isLiteral" -> true),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-model:recursive-item-v1"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1DATASET"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:oai-item1"),
        ("predicate" -> DATASET_ARCHIVAL_STORAGE_LOCATION) ~ ("object" -> s.bagStorageLocation) ~ ("isLiteral" -> true)
        ) ++ audiences.map(audience =>
          ("predicate" -> IS_MEMBER_OF) ~ ("object" -> s"info:fedora/${s.disciplines(audience)}"))
      ))

    for {
      audiences <- readAudiences()
      _ <- writeToFile(new File(sdoDir.getPath, JSON_CFG_FILENAME), pretty(render(sdoCfg(audiences))))
    } yield ()
  }

  def createFileCfg(fileLocation: String, mimeType: String, parentSDO: File): JObject =
    createFileCfg(fileLocation, mimeType, ("predicate" -> IS_MEMBER_OF) ~ ("objectSDO" -> parentSDO.getName))

  def createFileCfg(fileLocation: String, mimeType: String, parentObjectId: String): JObject =
    createFileCfg(fileLocation, mimeType, ("predicate" -> IS_MEMBER_OF) ~ ("object" -> s"info:fedora/$parentObjectId"))

  private def createFileCfg(fileLocation: String, mimeType: String, memberOfRelation: JObject): JObject = {
      ("namespace" -> "easy-file") ~
        ("datastreams" -> List(
          ("dsLocation" -> fileLocation) ~
            ("dsID" -> "EASY_FILE") ~
            ("controlGroup" -> "R") ~
            ("mimeType" -> mimeType)
          //        ("checksumType" -> "SHA-1") ~
          //        ("checksum" -> "d4484a61da2d91194f4401e2c761ba8b63bfeb29") // TODO: FILL IN REAL CHECKSUM !!!!
          ,
          ("contentFile" -> "EASY_FILE_METADATA") ~
            ("dsID" -> "EASY_FILE_METADATA") ~
            ("controlGroup" -> "X") ~
            ("mimeType" -> "text/xml"))) ~
        ("relations" -> List(
          memberOfRelation,
          ("predicate" -> IS_SUBORDINATE_TO) ~ ("objectSDO" -> DATASET_SDO),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
        ))
  }

  def createDirCfg(dirName: String, parentSDO: File): JObject = {
    createDirCfg(("predicate" -> IS_MEMBER_OF) ~ ("objectSDO" -> parentSDO.getName))
  }

  def createDirCfg(dirName: String, parentObjectId: String): JObject = {
    createDirCfg(("predicate" -> IS_MEMBER_OF) ~ ("object" -> s"info:fedora/$parentObjectId"))
  }

  private def createDirCfg(memberOfRelation: JObject): JObject = {
      ("namespace" -> "easy-folder") ~
        ("datastreams" -> List(
          ("contentFile" -> "EASY_ITEM_CONTAINER_MD") ~
            ("dsID" -> "EASY_ITEM_CONTAINER_MD") ~
            ("controlGroup" -> "X") ~
            ("mimeType" -> "text/xml"))) ~
        ("relations" -> List(
          memberOfRelation,
          ("predicate" -> IS_SUBORDINATE_TO) ~ ("objectSDO" -> DATASET_SDO),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FOLDER"),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
        ))
  }
}
