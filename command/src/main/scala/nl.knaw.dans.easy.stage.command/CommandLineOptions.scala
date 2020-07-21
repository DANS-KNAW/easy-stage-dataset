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
package nl.knaw.dans.easy.stage.command

import java.io.File
import java.net.URI
import java.nio.file.{ Path, Paths }
import java.util.regex.Pattern

import org.joda.time.DateTime
import org.rogach.scallop.{ ScallopConf, ScallopOption, ValueConverter, singleArgConverter }
import resource._

import scala.io.Source
import scala.util.Try

class CommandLineOptions(args: Seq[String], configuration: Configuration) extends ScallopConf(args) {

  appendDefaultToDescription = true
  editBuilder(sc => sc.setHelpWidth(110))

  printedName = "easy-stage-dataset"
  version(s"$printedName v${ configuration.version }")
  private val _________ = " " * printedName.length
  val description = "Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository."
  val synopsis: String =
    s"""$printedName -t <submission-timestamp> -u <urn> [ -d <doi> ] [ -o ] [ -f <external-file-uris> ] [-a <archive>] \\
       |${ _________ } <EASY-deposit> <staged-digital-object-set>""".stripMargin
  banner(
    s"""
       |$description
       |
       |Usage:
       |
       |$synopsis
       |
       |Options:
       |""".stripMargin)

  private implicit val dateTimeConv: ValueConverter[DateTime] = singleArgConverter[DateTime](conv = DateTime.parse)

  val submissionTimestamp: ScallopOption[DateTime] = opt[DateTime](
    name = "submission-timestamp", short = 't',
    descr = "Timestamp in ISO8601 format")
  val urn: ScallopOption[String] = opt[String](
    name = "urn", short = 'u',
    descr = "The URN to assign to the new dataset in EASY")
  val doi: ScallopOption[String] = opt[String](
    name = "doi", short = 'd',
    descr = "The DOI to assign to the new dataset in EASY. If omitted, no files are ingested into EASY, not even place holders.")
  val otherAccessDOI: ScallopOption[Boolean] = opt[Boolean](
    name = "doi-is-other-access-doi", short = 'o',
    descr = """Stage the provided DOI as an "other access DOI"""",
    default = Some(false))
  val dsLocationMappings: ScallopOption[File] = opt[File](
    name = "external-file-uris", short = 'f',
    descr = "File with mappings from bag local path to external file URI. Each line in this " +
      "file must contain a mapping. The path is separated from the URI by one ore more " +
      "whitespaces. If more groups of whitespaces are encountered, they are considered " +
      "part of the path.")
  val state: ScallopOption[String] = opt[String](
    name = "state",
    descr = "The state of the dataset to be created. This must be one of DRAFT, SUBMITTED or PUBLISHED.",
    default = Option("DRAFT"))
  val archive: ScallopOption[String] = opt[String](
    name = "archive",
    descr = "The way the dataset is archived. This must be either EASY or DATAVAULT. " +
      "EASY: Data and metadata are archived in EASY. " +
      "DATAVAULT: Data and metadata are archived in the DATAVAULT. There may be dissemination copies in EASY.",
    default = Option("EASY"))
  val includeBagMetadata: ScallopOption[Boolean] = opt[Boolean](
    name = "include-bag-metadata", short = 'i',
    descr = "Indicates whether bag metadata (such as dataset.xml and files.xml) should be included in the resultant staged digital object.",
    default = Some(false),
  )
  val deposit: ScallopOption[File] = trailArg[File](
    name = "EASY-deposit",
    descr = "Deposit directory contains deposit.properties file and bag with extra metadata for EASY to be staged for ingest into Fedora",
    required = true)
  val sdoSet: ScallopOption[File] = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)

  validateFileExists(deposit)
  verify()

  def getDsLocationMappings: Map[Path, URI] = {
    dsLocationMappings.map(readDsLocationMappings(_).get).getOrElse(Map.empty)
  }

  private val dsLocationsFileLinePattern = Pattern.compile("""^(.*)\s+(.*)$""")

  private def readDsLocationMappings(file: File): Try[Map[Path, URI]] = {
    managed(Source.fromFile(file, "UTF-8"))
      .map(_.getLines()
        .collect {
          case line if line.nonEmpty =>
            val m = dsLocationsFileLinePattern.matcher(line)
            if (m.find()) (Paths.get(m.group(1).trim), new URI(m.group(2).trim))
            else throw new IllegalArgumentException(s"Invalid line in input: '$line'")
        }
        .toMap)
      .tried
  }
}
