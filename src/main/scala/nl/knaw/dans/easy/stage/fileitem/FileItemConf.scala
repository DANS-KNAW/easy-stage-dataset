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
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.fileitem.FileItemSettings.{creatorRoles, accessCategories}
import nl.knaw.dans.easy.stage.lib.Version
import org.rogach.scallop._
import org.slf4j.LoggerFactory

class FileItemConf(args: Seq[String]) extends ScallopConf(args) {
  val log = LoggerFactory.getLogger(getClass)

  printedName = "easy-stage-file-item"
  version(s"$printedName v${Version()}")
  banner(s"""Stage a file item for ingest into a datasaet in an EASY Fedora Commons 3.x Repository.
            |
            |Usage:
            |
            | $printedName [<options>...] <staged-digital-object-set>
            | $printedName <staged-digital-object-set> <csv-file>
            |
            |Options:
            |""".stripMargin)

  val httpUrl: ValueConverter[URL] = singleArgConverter[URL](s => {
    val result = new URL(s)
    if(result.getProtocol == null || !result.getProtocol.startsWith("http"))
      throw new IllegalArgumentException(s"$s should have protocol http")
    result
  })
  val mayNotExist = singleArgConverter[File](conv = new File(_))
  val shouldBeFile = singleArgConverter[File](conv = {f =>
    if (!new File(f).isFile) throw new IllegalArgumentException(s"$f is not an existing file")
    new File(f)
  })
  val accessCategory = singleArgConverter[String](conv = { s =>
    if (!accessCategories.contains(s) && !s.isEmpty) throw new IllegalArgumentException(s"$s is not a valid access category")
    s
  })
  val roles = singleArgConverter[String](conv = {s =>
    if (!creatorRoles.contains(s) && !s.isEmpty) throw new IllegalArgumentException(s"$s is not a valid creator role")
    s
  })

  val pathInDataset = opt[File](
    name = "path-in-dataset", short = 'p',
    descr = "the path that the file should get in the dataset, a staged digital object is created" +
      " for the file and the ancestor folders that don't yet exist in the dataset")(mayNotExist)
  val format = opt[String](
    name = "format", short = 'f',
    descr = "dcterms property format, the mime type of the file",
    default = Some("application/octet-stream"))
  val dsLocation = opt[URL](
    name = "datastream-location",
    descr = "http URL to redirect to")(httpUrl)
  val size = opt[Long](
    name = "size",
    descr = "Size in bytes of the file data")
  val datasetId = opt[String](
    name = "dataset-id", short = 'i',
    descr = "id of the dataset in Fedora that should receive the file to stage (requires file-path). " +
     "If omitted the trailing argument csv-file is required")
  val accessibleTo = opt[String] (
    name = "accessible-to", short = 'a',
    descr = s"specifies the accessibility of the file item; either one of [${accessCategories.mkString(",")}]",
    default = Some("ANONYMOUS"))//(accessCategory)
  val visibleTo = opt[String] (
    name = "visible-to", short = 'v',
    descr = s"specifies the visibility of the file item; either one of [${accessCategories.mkString(",")}",
    default = Some("ANONYMOUS"))//(accessCategory)
  val creatorRole = opt[String](
    name = "creator-role", short = 'c',
    descr = s"specifies the role of the file item creator; either one of [[${creatorRoles.mkString(",")}]",
    default = Some("NONE"))//(roles)
  val ownerId = opt[String](
    name = "owner-id", noshort = true,
    descr = "specifies the id of the owner/creator of the file item " +
      "(defaults to the one configured in the application configuration file)"
  )
  val csvFile = trailArg[File](
    name = "csv-file",
    descr = "a comma separated file with one column for each option " +
     "(additional columns are ignored) and one set of options per line",
    required = false)(shouldBeFile)
  val sdoSetDir = trailArg[File](
    name = "staged-digital-object-sets",
    descr = "The resulting directory with Staged Digital Object directories per dataset" +
      " (will be created if it does not exist)",
    required = true)(mayNotExist)

  mainOptions = Seq(datasetId, dsLocation, pathInDataset, size, format)

  dependsOnAll(format,List(datasetId,pathInDataset,size,dsLocation))
  dependsOnAll(datasetId,List(pathInDataset,size,dsLocation))
  conflicts(csvFile,List(datasetId,pathInDataset,size,dsLocation))
  requireOne(csvFile,datasetId)

  val longOptionNames = builder.opts.filter(!_.isInstanceOf[TrailingArgsOption]).map(_.name)

  override def toString = builder.args.mkString(", ")
}

object FileItemConf {
  val dummy = new FileItemConf("-ii -dhttp:// -pp -s0 --format f outdir".split(" "))
}
