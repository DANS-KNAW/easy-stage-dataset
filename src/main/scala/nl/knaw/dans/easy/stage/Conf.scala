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
import java.net.URL

import nl.knaw.dans.easy.stage.lib.Version
import org.joda.time.DateTime
import org.rogach.scallop.{ScallopConf, ScallopOption, singleArgConverter}
import org.slf4j.LoggerFactory



class Conf(args: Seq[String]) extends ScallopConf(args) {
  val log = LoggerFactory.getLogger(getClass)

  editBuilder(sc => sc.setHelpWidth(110))
  appendDefaultToDescription = true

  printedName = "easy-stage-dataset"
  version(s"$printedName v${Version()}")

  private val _________ = printedName.map(_ => " ").mkString("")

  val description = """Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository."""
  val synopsis =
    s"""  $printedName -t <submission-timestamp> -u <urn> -d <doi> [ -o ] [ -m ] \\
       |  ${_________}    <EASY-bag> <staged-digital-object-set>""".stripMargin
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
  val otherAccessDOI = opt[Boolean](
    name = "doi-is-other-access-doi", short = 'o',
    descr = """Stage the provided DOI as an "other access DOI"""",
    default = Some(false))
  val stageFileDataAsRedirectDatastreams = opt[Boolean](
    name = "stage-file-data-as-redirect-datastreams", short = 'r',
    descr = """Stage file items so that the content of file data will NOT be stored in managed Fedora Storage""",
    default = Some(false))
  val fileDataRedirectBaseUrl = opt[URL](
    name = "file-data-redirect-base-url", short = 'b',
    descr = """Base URL from which redirect URLs to the file items in this dataset will be constructed""",
    default = None)
  val deposit = trailArg[File](
    name = "EASY-deposit",
    descr = "Deposit directory contains deposit.properties file and bag with extra metadata for EASY to be staged for ingest into Fedora",
    required = true)
  val sdoSet = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)

  validateFileExists(deposit)
  dependsOnAll(fileDataRedirectBaseUrl, List(stageFileDataAsRedirectDatastreams))

  verify()
}
