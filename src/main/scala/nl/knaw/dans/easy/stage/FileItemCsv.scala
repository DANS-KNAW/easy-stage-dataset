package nl.knaw.dans.easy.stage

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser.parse

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source.fromInputStream


object FileItemCsv {
  lazy val csv: mutable.Buffer[List[String]] = {
    val rawContent = fromInputStream(System.in).mkString
    val parser = parse(rawContent, CSVFormat.RFC4180)
    parser.getRecords.map(_.toList)
  }
}
