package nl.knaw.dans.easy.stage

import java.io.File

import org.joda.time.DateTime
import org.rogach.scallop.{singleArgConverter, ValueConverter, ScallopConf}

class Conf(args: Seq[String]) extends ScallopConf(args) {
  printedName = "easy-stage-dataset"
  val indent_____ = printedName.replaceAll(".", " ")
  version(s"$printedName v${Version()}")
  banner(s"""
           |Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.
           |
           |Usage:
           |
           | $printedName -t <submission-timestamp> -u <urn> -d <doi> [ -o ] \\
           | $indent_____    <EASY-bag> <staged-digital-object-set>
           |
           |Options:
           |""".stripMargin)

  implicit val conv: ValueConverter[DateTime] = singleArgConverter[DateTime](conv = DateTime.parse)

  val submissionTimestamp = opt[DateTime](
    name = "submission-timestamp", short = 't',
    descr = "Timestamp in ISO8601 format",
    required = true)
  val urn = opt[String](
    name = "urn", short = 'u',
    descr = "The URN to assign to the new dataset in EASY",
    required = true)
  val doi = opt[String](
    name = "doi", short = 'd',
    descr = "The DOI to assign to the new dataset in EASY",
    required = true)
  val otherAccessDOI = opt[Boolean](
    name = "doi-is-other-access-doi", short = 'o',
    descr = """Stage the provided DOI as an "other access DOI"""",
    default = Some(false))
  val bag = trailArg[File](
    name = "EASY-bag",
    descr = "Bag with extra metadata for EASY to be staged for ingest into Fedora",
    required = true)
  val sdoSet = trailArg[String](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)
}
