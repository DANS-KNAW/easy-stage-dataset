package nl.knaw.dans.easy.stage

import java.io.File

import org.rogach.scallop.ScallopConf

class Conf(args: Seq[String]) extends ScallopConf(args) {
  printedName = "easy-ingest"
  version(s"$printedName ${Version()}")
  banner("""
           |Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.
           |
           |Usage: easy-stage-dataset  <EASY-bag> <staged-digital-object-set> <urn>
           |Options:
           |""".stripMargin)

  val bag = trailArg[File](
    name = "EASY-bag",
    descr = "The directory in EASY-BagIt format to be staged for ingest in Fedora",
    required = true)
  val sdoSet = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)
  val urn = trailArg[String](
    name = "urn",
    descr = "The URN to assign tot the new dataset in EASY",
    required = true)
}
