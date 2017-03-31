/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage

import java.io.File
import java.net.{URI, URL}
import java.nio.file.{Path, Paths}
import java.util.regex.Pattern

import nl.knaw.dans.easy.stage.lib.Version
import org.joda.time.DateTime
import org.rogach.scallop.{ScallopConf, ScallopOption, singleArgConverter}
import org.slf4j.LoggerFactory

import scala.io.Source
import scala.util.Try

class Conf(args: Seq[String]) extends ScallopConf(args) {
  private val log = LoggerFactory.getLogger(getClass)

  editBuilder(sc => sc.setHelpWidth(110))
  appendDefaultToDescription = true

  printedName = "easy-stage-dataset"
  version(s"$printedName v${Version()}")

  private val _________ = printedName.map(_ => " ").mkString("")

  val description = """Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository."""
  val synopsis: String =
    s"""  $printedName -t <submission-timestamp> -u <urn> -d <doi> [ -o ] [ -m ] \\
       |  ${_________}    <EASY-deposit> <staged-digital-object-set>""".stripMargin
  banner(s"""
           |  $description
           |
           |Usage:
           |
           |$synopsis
           |
           |Options:
           |""".stripMargin)

  implicit val dateTimeConv = singleArgConverter[DateTime](conv = DateTime.parse)

  val submissionTimestamp: ScallopOption[DateTime] = opt[DateTime](
    name = "submission-timestamp", short = 't',
    descr = "Timestamp in ISO8601 format")
  val urn: ScallopOption[String] = opt[String](
    name = "urn", short = 'u',
    descr = "The URN to assign to the new dataset in EASY")
  val doi: ScallopOption[String] = opt[String](
    name = "doi", short = 'd',
    descr = "The DOI to assign to the new dataset in EASY")
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

  def getDsLocationMappings(): Map[Path, URI] = {
    if(dsLocationMappings.isSupplied) readDsLocationMappings(dsLocationMappings()).get
    else Map()
  }

  val dsLocationsFileLinePattern = Pattern.compile("""^(.*)\s+(.*)$""")

  private def readDsLocationMappings(file: File) = Try {
    resource.managed(Source.fromFile(file, "UTF-8")).acquireAndGet (
      _.getLines().toList.filter(_.nonEmpty).map {
        line =>
          val m = dsLocationsFileLinePattern.matcher(line)
          if (m.find()) (Paths.get(m.group(1).trim), new URI(m.group(2).trim))
          else throw new IllegalArgumentException(s"Invalid line in input: '$line'")
      }.toMap)
  }
}
