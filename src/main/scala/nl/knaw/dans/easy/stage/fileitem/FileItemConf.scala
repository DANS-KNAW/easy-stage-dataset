/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.fileitem.FileAccessRights.UserCategory
import nl.knaw.dans.easy.stage.fileitem.FileItemConf._
import nl.knaw.dans.easy.stage.fileitem.FileItemSettings._
import nl.knaw.dans.easy.stage.lib.Version
import org.rogach.scallop._
import org.slf4j.LoggerFactory

class FileItemConf(args: Seq[String]) extends ScallopConf(args) {
  val log = LoggerFactory.getLogger(getClass)

  editBuilder(_.setHelpWidth(110))
  appendDefaultToDescription = true

  printedName = "easy-stage-file-item"
  version(s"$printedName v${Version()}")

  val synopsis = s"""$printedName [<options>...] <staged-digital-object-set>"""
  val description = """Stage a file item for ingest into a datasaet in an EASY Fedora Commons 3.x Repository."""

  banner(s"""
            |  $description
            |
            |Usage:
            |
            |  $synopsis
            |
            |Options:
            |""".stripMargin)

  def userCategory(default: FileAccessRights.Value) = singleArgConverter(s => {
    if (s.trim.isEmpty) default else FileAccessRights.valueOf(s).get
  })
  def emptyIsDefault(default: String) = singleArgConverter(s => {
    if (s.trim.isEmpty) default else s
  })

  val pathInDataset = opt[File](name = "path-in-dataset", short = 'p',
    descr = "the path that the file should get in the dataset, a staged digital object is created" +
      " for the file and the ancestor folders that don't yet exist in the dataset")
  val format = opt[String](name = "format", short = 'f',
    descr = s"dcterms property format, the mime type of the file")
  val dsLocation = opt[URL](name = "datastream-location",
    descr = "http URL to redirect to (if specified, file-location MUST NOT be specified)")
  val size = opt[Long](name = "size", descr = "Size in bytes of the file data")
  val file = opt[File](name = "file-location", short = 'l',
    descr = "The file to be staged (if specified, --datastream-location is ignored)")
  val datasetId = opt[String](name = "dataset-id", short = 'i',
    descr = "id of the dataset in Fedora that should receive the file to stage (requires file-path). " +
     "If omitted the trailing argument csv-file is required")
  val accessibleTo = opt[UserCategory](name = "accessible-to", short = 'a',
    descr = s"specifies the accessibility of the file item; one of: ${FileAccessRights.values.mkString(", ")}",
    default = Some(FileAccessRights.NONE))(userCategory(FileAccessRights.NONE))
  val visibleTo = opt[UserCategory](name = "visible-to", short = 'v',
    descr = s"specifies the visibility of the file item; one of: ${FileAccessRights.values.mkString(", ")}",
    default = Some(FileAccessRights.ANONYMOUS))(userCategory(FileAccessRights.ANONYMOUS))
  val creatorRole = opt[String](name = "creator-role", short = 'c',
    descr = s"specifies the role of the file item creator; one of: ${creatorRoles.mkString(", ")}",
    default = Some(defaultCreatorRole))(emptyIsDefault(defaultCreatorRole))

  val ownerId = opt[String](name = "owner-id", noshort = true,
    descr = "specifies the id of the owner/creator of the file item " +
      "(defaults to the one configured in the application configuration file)")
  val csvFile = opt[File](name = "csv-file",
    descr = "a comma separated file with one column for each option " +
      "(additional columns are ignored) and one set of options per line")
  val sdoSetDir = trailArg[File](name = "staged-digital-object-sets", required = true,
    descr = "The resulting directory with Staged Digital Object directories per dataset" +
      " (will be created if it does not exist)")

  mainOptions = Seq(datasetId, dsLocation, pathInDataset, size, format)

  requireOne(csvFile, datasetId)
  conflicts(csvFile, List(datasetId, pathInDataset, size, file, dsLocation))
  dependsOnAll(datasetId, List(pathInDataset, size, format))
  dependsOnAny(datasetId, List(dsLocation, file))
  conflicts(dsLocation, List(file))

  validate(dsLocation)(url => {
    if (url.getProtocol == null || !url.getProtocol.startsWith("http"))
      Left("$url should have protocol http")
    else Right(())
  })

  validate(creatorRole)(validateValue(_, creatorRoles))

  validateFileExists(file)
  validateFileIsFile(file)

  validateFileExists(csvFile)
  validateFileIsFile(csvFile)

  val longOptionNames = builder.opts
    .filter(!_.isInstanceOf[TrailingArgsOption])
    .filter(_.name != "csv-file")
    .map(_.name)

  override def toString = builder.args.mkString(", ")
  verify()
}

object FileItemConf {
  val dummy = new FileItemConf("-i i -d http:// -p p -s 0 --format f outdir".split(" "))

  def validateValue(actualValue: String, expectedValues: Array[String]): Either[String, Unit] = {
    if (expectedValues.contains(actualValue))
      Right(())
    else
      Left(s"got '$actualValue' but expected one of $expectedValues")
  }
}
