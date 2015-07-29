package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.Util._
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.Try

object JSON {

  def createFileCfg(fileLocation: String, mimeType: String, parentSDO: String, sdoDir: File): Try[Unit] = {
    val sdoCfg =
      ("namespace" -> "easy-file") ~
      ("datastreams" -> List(
        ("dsLocation" -> fileLocation ) ~
          ("dsID" -> "EASY_FILE") ~
          ("controlGroup" -> "R") ~
          ("mimeType" -> mimeType))) ~
      ("relations" -> List(
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isMemberOf") ~ ("objectSDO" -> parentSDO),
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isSubordinateTo") ~ ("objectSDO" -> DATASET_SDO),
        ("predicate" -> "info:fedora/fedora-system:def/model#") ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
        ("predicate" -> "info:fedora/fedora-system:def/model#") ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    writeToFile(new File(sdoDir.getPath, JSON_CFG_FILENAME), pretty(render(sdoCfg)))
  }

  def createDirCfg(dirName: String, parentSDO: String, sdoDir: File): Try[Unit] = {
    val sdoCfg =
      ("namespace" -> "easy-folder") ~
      ("relations" -> List(
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isMemberOf") ~ ("objectSDO" -> parentSDO),
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isSubordinateTo") ~ ("objectSDO" -> DATASET_SDO),
        ("predicate" -> "info:fedora/fedora-system:def/model#hasModel") ~ ("object" -> "info:fedora/easy-model:EDM1FOLDER"),
        ("predicate" -> "info:fedora/fedora-system:def/model#hasModel") ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    writeToFile(new File(sdoDir.getPath, JSON_CFG_FILENAME), pretty(render(sdoCfg)))
  }

}
