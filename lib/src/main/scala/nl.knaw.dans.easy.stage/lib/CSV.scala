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

import java.io.{ File, FileInputStream, InputStream }

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser.parse

import scala.collection.JavaConverters._
import scala.io.Source.fromInputStream
import scala.util.{ Failure, Success, Try }

class CSV private(csv: Seq[Seq[String]], requiredHeaders: Seq[String]) {

  private val actualHeaders = csv.head.map(
    key => key.toLowerCase
  )

  /** @return a row for each line that has a value for at least one required column,
   *          each row has command-line like args (key-value pairs with long option names) for
   *          required columns as far as provided */
  def getRows: Seq[Seq[String]] =
    csv.tail.map(value => actualHeaders.zip(value)
      // ignore columns with headers that are not options
      .withFilter { case (header, content) => requiredHeaders.contains(header) && content.nonEmpty }
      // convert to command line options
      .flatMap { case (header, content) => Array(s"--$header", content) }
    ).filter(_.nonEmpty)
}

object CSV {

  /** @return (new instance, warning) */
  def apply(file: File, requiredHeaders: Seq[String]): Try[(CSV, Option[String])] = {
    apply(new FileInputStream(file), requiredHeaders)
      .recoverWith {
        case e: Exception =>
          Failure(new Exception(s"${ file.getAbsolutePath } : ${ e.getMessage }", e))
      }
      .map { case (csv, warning) => (csv, warning.map(s => s"${ file.getAbsolutePath } : $s")) }
  }

  /** @return (new instance, warning) */
  def apply(in: InputStream, requiredHeaders: Seq[String]): Try[(CSV, Option[String])] = {
    requiredHeaders.map(_.toLowerCase) match {
      case `requiredHeaders` => getContent(in)
        .flatMap(csv => {
          val upperCaseActualHeaders = csv.head.map(_.toUpperCase)
          val upperCaseRequiredHeaders = requiredHeaders.map(_.toUpperCase)
          val ignoredHeaders = upperCaseActualHeaders.filter(!upperCaseRequiredHeaders.contains(_))
          val missingHeaders = upperCaseRequiredHeaders.filter(!upperCaseActualHeaders.contains(_))

          if (missingHeaders.nonEmpty)
            Failure(new Exception(s"Missing columns: ${ missingHeaders.mkString(", ") }"))
          else
            Success((new CSV(csv, requiredHeaders), Some(s"Ignored columns: ${ ignoredHeaders.toArray.deep }")))
        })
      case _ => Failure(new Exception("required headers with uppercase are not supported " +
        "because we right-case the generated commandline options to lowercase"))
    }
  }

  private def getContent(in: InputStream): Try[Seq[Seq[String]]] = Try {
    parse(fromInputStream(in).mkString, CSVFormat.RFC4180).getRecords.asScala.map(_.asScala.toSeq)
  }
}
