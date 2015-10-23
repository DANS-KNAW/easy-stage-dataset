package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.Util._
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

  def createDatasetCfg(sdoDir: File)(implicit s: Settings): Try[Unit] = {
    def sdoCfg(audiences: Seq[String]) =
      ("namespace" -> "easy-dataset") ~
      ("datastreams" -> List(
        ("contentFile" -> "AMD") ~
        ("dsID" -> "AMD") ~
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
        ("mimeType" -> "text/xml")
      )) ~
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

  def createFileCfg(fileLocation: String, mimeType: String, parentSDO: String, sdoDir: File): Try[Unit] = {
    val sdoCfg =
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
        ("predicate" -> IS_MEMBER_OF) ~ ("objectSDO" -> parentSDO),
        ("predicate" -> IS_SUBORDINATE_TO) ~ ("objectSDO" -> DATASET_SDO),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    writeToFile(new File(sdoDir.getPath, JSON_CFG_FILENAME), pretty(render(sdoCfg)))
  }

  def createDirCfg(dirName: String, parentSDO: String, sdoDir: File): Try[Unit] = {
    val sdoCfg =
      ("namespace" -> "easy-folder") ~
      ("datastreams" -> List(
        ("contentFile" -> "EASY_ITEM_CONTAINER_MD") ~
        ("dsID" -> "EASY_ITEM_CONTAINER_MD") ~
        ("controlGroup" -> "X") ~
        ("mimeType" -> "text/xml"))) ~
      ("relations" -> List(
        ("predicate" -> IS_MEMBER_OF) ~ ("objectSDO" -> parentSDO),
        ("predicate" -> IS_SUBORDINATE_TO) ~ ("objectSDO" -> DATASET_SDO),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FOLDER"),
        ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    writeToFile(new File(sdoDir.getPath, JSON_CFG_FILENAME), pretty(render(sdoCfg)))
  }

}
