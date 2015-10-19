package nl.knaw.dans.easy.stage

import java.io.File

import org.joda.time.DateTime
import org.rogach.scallop.{singleArgConverter, ValueConverter, ScallopConf}

class Conf(args: Seq[String] = "-b. -s. -t2015 -uu -dd".split(" ")) extends ScallopConf(args) {
  printedName = "easy-stage-dataset"
  version(s"$printedName ${Version()}")
  banner(s"""
           |Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.
           |
           |Usage:
           |
           | $printedName -b <EASY-bag> -s <staged-digital-object-set> -u <urn> -d <doi> -t <submission-timestamp> [ -o ]
           |
           |Options:
           |""".stripMargin)

  implicit val conv: ValueConverter[DateTime] = singleArgConverter[DateTime](conv = DateTime.parse)

  val bag = opt[File](
    name = "EASY-bag", short = 'b',
    descr = "Bag with extra metadata for EASY to be staged for ingest into Fedora",
    required = true)
  val sdoSet = opt[File](
    name = "staged-digital-object-set", short = 's',
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)(singleArgConverter[File](conv = new File(_)))
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
    name = "stage-other-access-doi", short = 'o',
    descr = "Stage the provided DOI as an \"other access DOI\"",
    default = Some(false))
}
