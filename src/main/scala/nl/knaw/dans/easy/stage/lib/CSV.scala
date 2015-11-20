package nl.knaw.dans.easy.stage.lib

import java.io.{File, FileInputStream, InputStream}

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser.parse

import scala.collection.JavaConversions._
import scala.io.Source.fromInputStream
import scala.util.{Failure, Success, Try}

class CSV private (csv: Seq[Seq[String]], requiredHeaders: Seq[String]) {

  private val actualHeaders = csv.head.map(
    key => key.toLowerCase
  )

  /** @return a row for each line that has a value for at least one required column,
    *          each row has command-line like args (key-value pairs with long option names) for
    *          required columns as far as provided */
  def getRows: Seq[Seq[String]] =
    csv.tail.map(value => actualHeaders.zip(value)
      // ignore columns with headers that are not options
      .filter(t => requiredHeaders.contains(t._1))
      // convert to command line options
      .flatMap(t => Array("--" + t._1, t._2))
    ).filter(args => args != Nil)
}

object CSV {

  /** @return (new instance, warning) */
  def apply (file: File, requiredHeaders: Seq[String]): Try[(CSV, Option[String])] =
    apply(new FileInputStream(file),requiredHeaders).recoverWith{
      case e: Exception =>
        Failure(new Exception(s"${file.getAbsolutePath} : ${e.getMessage}", e))
    }.map { case (csv, warning) => (csv, warning.map(s => s"${file.getAbsolutePath} : $s")) }

  /** @return (new instance, warning) */
  def apply(in: InputStream, requiredHeaders: Seq[String]): Try[(CSV, Option[String])] =
    if (requiredHeaders.map(_.toLowerCase)!=requiredHeaders)
      Failure(new Exception("required headers with uppercase are not supported " +
        "because we right-case the generated commandline options to lowercase"))
    else getContent(in).flatMap(csv => {
      val upperCaseActualHeaders = csv.head.map(_.toUpperCase)
      val upperCaseRequiredHeaders = requiredHeaders.map(_.toUpperCase)
      val ignoredHeaders = upperCaseActualHeaders.filter(!upperCaseRequiredHeaders.contains(_))
      val missingHeaders = upperCaseRequiredHeaders.filter(!upperCaseActualHeaders.contains(_))

      if (missingHeaders.nonEmpty)
        Failure(new Exception(s"Missing columns: ${missingHeaders.mkString(", ")}"))
      else Success((
        new CSV(csv,requiredHeaders),
        Some (s"Ignored columns: ${ignoredHeaders.toArray.deep}")
        ))
    })

  private def getContent(in: InputStream): Try[Seq[Seq[String]]] = Try {

    val rawContent = fromInputStream(in).mkString
    parse(rawContent, CSVFormat.RFC4180).getRecords.map(_.toList)
  }
}
