package nl.knaw.dans.easy.stage

import java.io.File

import org.joda.time.DateTime
import org.rogach.scallop.ScallopConf

class Conf(args: Seq[String]) extends ScallopConf(args) {
  printedName = "easy-state-dataset"
  version(s"$printedName ${Version()}")
  banner("""
           |Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.
           |
           |Usage: easy-stage-dataset  <EASY-bag> <staged-digital-object-set> <urn> <submission-timestamp>
           |Options:
           |""".stripMargin)

  val bag = trailArg[File](
    name = "EASY-bag",
    descr = "Bag with extra metadata for EASY to be staged for ingest into Fedora",
    required = true)
  /*
   * Scallop fails with a vague error if a type File argument refers to a file that does
   * not (yet) exist. As the SDO-set might not yet exist we work around this by making
   * this a type String argument
   */
  val sdoSet = trailArg[String](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)
  val urn = trailArg[String](
    name = "urn",
    descr = "The URN to assign tot the new dataset in EASY",
    required = true)
  val submissionTimestamp = trailArg[String](
    name = "submission-timestamp",
    descr = "Timestamp in ISO8601 format",
    required = true
  )
  validateOpt(submissionTimestamp) {
    case Some(t) =>
      try {
        DateTime.parse(t)
        Right(Unit)
      } catch {
        case e: IllegalArgumentException => Left(s"Not a valid ISO8601 date: $t. Error: ${e.getMessage}")
      }
    case _ => Left("Could not parse argument submission-timestamp ")
  }
}
