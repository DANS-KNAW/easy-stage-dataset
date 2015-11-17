package nl.knaw.dans.easy.stage.lib

import java.io.{File, FileInputStream, InputStream}

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser.parse
import org.rogach.scallop.{ScallopConf, TrailingArgsOption}

import scala.collection.JavaConversions._
import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

class CSV(csv: Seq[Seq[String]]) {

  /** @return a key-value pair per column for an input line */
  def getRows = csv.tail.map(value =>
    csv.head.map(
      key => key.toLowerCase
    ).zip(value)
      // ignore columns with headers that are not options
      .filter(t => !csv.head.contains(t._1))
      // convert to command line options
      .flatMap(t => Array("--" + t._1, t._2))
  ).filter(args => args != Nil)
}


object CSV {

  def apply(in: File, conf: ScallopConf): Try[(Seq[String],CSV)] =
    getContent(new FileInputStream(in)).flatMap(csv => {
      val head = csv.head
      val requiredHeaders = conf.builder.opts.filter(!_.isInstanceOf[TrailingArgsOption])
        .map(_.name).map(_.toUpperCase)
      val ignoredHeaders = head.filter(!requiredHeaders.contains(_))
      val missingHeaders = requiredHeaders.filter(!head.toArray.contains(_))

      if (missingHeaders.nonEmpty)
        Failure(new Exception(s"Missing columns: ${missingHeaders.mkString(", ")}"))
      else Success((ignoredHeaders,new CSV(csv)))
    })

  private def getContent(in: InputStream): Try[Seq[Seq[String]]] = Try {

    val rawContent = fromInputStream(in).mkString
    parse(rawContent, CSVFormat.RFC4180).getRecords.map(_.toList)
  }
}
