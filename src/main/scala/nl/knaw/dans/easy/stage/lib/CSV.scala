package nl.knaw.dans.easy.stage.lib

import java.io.{File, FileInputStream, InputStream}

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser.parse
import org.rogach.scallop.{ScallopConf, TrailingArgsOption}
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.collection.mutable
import scala.io.Source.fromInputStream
import scala.util.{Success, Failure, Try}

object CSV {
  val log = LoggerFactory.getLogger(getClass)

  def apply(in: File, conf: ScallopConf): Try[Seq[Seq[String]]] =
    getContent(new FileInputStream(in)).flatMap(csv => {
      val head = csv.head
      val requiredHeaders = conf.builder.opts.filter(!_.isInstanceOf[TrailingArgsOption])
        .map(_.name).map(_.toUpperCase)
      val ignoredHeaders = head.filter(!requiredHeaders.contains(_))
      val missingHeaders = requiredHeaders.filter(!head.toArray.contains(_))
      if (ignoredHeaders.nonEmpty)
        log.warn(s"Ignoring columns: ${ignoredHeaders.toArray.mkString(", ")}")

      if (missingHeaders.nonEmpty)
        Failure(new Exception(s"Missing columns: ${missingHeaders.mkString(", ")}"))
      else Success(csv.tail.map(value =>
        // a key-value pair per column for an input line
        head.map(key => key.toLowerCase).zip(value)
          // ignore columns with headers that are not options
          .filter(t => !head.contains(t._1))
          // convert to command line options
          .flatMap(t => Array("--" + t._1, t._2))
      ).filter(args => args != Nil))
    })

  private def getContent(in: InputStream): Try[mutable.Buffer[List[String]]] = Try {

    val rawContent = fromInputStream(in).mkString
    parse(rawContent, CSVFormat.RFC4180).getRecords.map(_.toList)
  }
}
