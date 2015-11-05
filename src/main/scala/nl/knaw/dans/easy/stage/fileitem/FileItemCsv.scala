package nl.knaw.dans.easy.stage.fileitem

import java.io.InputStream

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser.parse
import org.rogach.scallop.TrailingArgsOption
import org.slf4j.LoggerFactory

import scala.collection.JavaConversions._
import scala.io.Source.fromInputStream
import scala.util.Try

object FileItemCsv {
  val log = LoggerFactory.getLogger(getClass)

  def read(in: InputStream, conf: FileItemConf): Try[Array[FileItemSettings]] = Try {
    val trailArgs: List[String] = List(conf.sdoSetDir.apply().toString)
    val csv = {
      val rawContent = fromInputStream(in).mkString
      parse(rawContent, CSVFormat.RFC4180).getRecords.map(_.toList)
    }
    val head = csv.head
    validateHeaders(conf,head)
    csv.tail.map(value =>
      // a key-value pair per column for an input line
      head.map(key => key.toLowerCase).zip(value)
      // ignore columns with headers that are not options
      .filter(t => !head.contains(t._1))
      // convert to command line options
      .flatMap(t => Array("--" + t._1, t._2))
    ).filter(args => args != Nil).map(args =>
      FileItemSettings(Array(args, trailArgs).flatten)
    ).toArray
  }

  private def validateHeaders(conf: FileItemConf, headers: List[String]): Unit = {
    val requiredHeaders = conf.builder.opts
      .filter(!_.isInstanceOf[TrailingArgsOption])
      .map(_.name).map(_.toUpperCase)
    val ignoredHeaders = headers.filter(!requiredHeaders.contains(_))
    val missingHeaders = requiredHeaders.filter(!headers.toArray.contains(_))
    if (ignoredHeaders.nonEmpty)
      log.warn(s"Ignoring columns: ${ignoredHeaders.toArray.mkString(", ")}")
    if (missingHeaders.nonEmpty)
      throw new scala.Exception(s"Missing columns: ${missingHeaders.mkString(", ")}")
  }
}
