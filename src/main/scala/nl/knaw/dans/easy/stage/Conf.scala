package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.EasyStageDataset._
import org.joda.time.DateTime
import org.rogach.scallop.{singleArgConverter, ValueConverter, ScallopConf}
import org.slf4j.LoggerFactory

class Conf(args: Seq[String]) extends ScallopConf(args) {
  val log = LoggerFactory.getLogger(getClass)

  printedName = "easy-stage-dataset"
  version(s"$printedName v${Version()}")
  private val _________ = printedName.map(_ => " ").mkString("")
  banner(s"""
           |Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.
           |
           |Usage:
           |
           | $printedName -t <submission-timestamp> -u <urn> -d <doi> [ -o ] \\
           | ${_________}    <EASY-bag> <staged-digital-object-set>
           |
           |Options:
           |""".stripMargin)

  implicit val dateTimeConv: ValueConverter[DateTime] = singleArgConverter[DateTime](conv = DateTime.parse)
  val mayNotExist = singleArgConverter[File](conv = new File(_))
  val shouldExist = singleArgConverter[File](conv = {f =>
    if (!new File(f).isDirectory) {
      log.error(s"$f is not an existing directory")
      throw new IllegalArgumentException()
    }
    new File(f)
  })

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
    required = true)(shouldExist)
  val sdoSet = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)(mayNotExist)
}
