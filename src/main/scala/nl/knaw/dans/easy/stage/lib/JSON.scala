package nl.knaw.dans.easy.stage.lib

import nl.knaw.dans.easy.stage.Settings
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

object JSON {
  val HAS_DOI = "http://dans.knaw.nl/ontologies/relations#hasDoi"
  val HAS_PID = "http://dans.knaw.nl/ontologies/relations#hasPid"
  val HAS_MODEL = "info:fedora/fedora-system:def/model#hasModel"
  val IS_MEMBER_OF = "http://dans.knaw.nl/ontologies/relations#isMemberOf"
  val IS_SUBORDINATE_TO = "http://dans.knaw.nl/ontologies/relations#isSubordinateTo"
  val DATASET_ARCHIVAL_STORAGE_LOCATION = "http://dans.knaw.nl/ontologies/relations#datasetArchivalStorageLocation"

  def createDatasetCfg(mimeType: Option[String], audiences: Seq[String])(implicit s: Settings): String = {

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
        ("predicate" -> DATASET_ARCHIVAL_STORAGE_LOCATION) ~ ("object" -> s.bagStorageLocation) ~ ("isLiteral" -> true)
        ) ++ audiences.map(audience =>
          ("predicate" -> IS_MEMBER_OF) ~ ("object" -> s"info:fedora/${s.disciplines(audience)}"))
      ))

    pretty(render(sdoCfg(audiences)))
  }

  def createFileCfg(fileLocation: String,
                    mimeType: String,
                    parent: (String,String),
                    subordinate: (String,String)
                   ): String = {
      val json = ("namespace" -> "easy-file") ~
        ("datastreams" -> List(
          ("dsLocation" -> fileLocation) ~
            ("dsID" -> "EASY_FILE") ~
            ("controlGroup" -> "R") ~
            ("mimeType" -> mimeType)
          ,
          ("contentFile" -> "EASY_FILE_METADATA") ~
            ("dsID" -> "EASY_FILE_METADATA") ~
            ("controlGroup" -> "X") ~
            ("mimeType" -> "text/xml"))) ~
        ("relations" -> List(
          ("predicate" -> IS_MEMBER_OF) ~ parent,
          ("predicate" -> IS_SUBORDINATE_TO) ~ subordinate,
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
          ("predicate" -> HAS_MODEL) ~ ("object" -> "info:fedora/dans-container-item-v1")
        ))
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
