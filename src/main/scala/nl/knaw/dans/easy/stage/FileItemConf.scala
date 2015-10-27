package nl.knaw.dans.easy.stage

import java.io.File

import org.rogach.scallop._
import org.slf4j.LoggerFactory

class FileItemConf(args: Array[String]) extends ScallopConf {
  val log = LoggerFactory.getLogger(getClass)

  printedName = "easy-stage-file-item"
  version(s"$printedName v${Version()}")
  banner(s"""
            |Stage a file item in EASY-BagIt format for ingest into a datasaet in an EASY Fedora Commons 3.x Repository.
            |
            |Usage:
            |
            | $printedName <options>... <EASY-bag> <staged-digital-object-set>
            | or
            | $printedName <EASY-bag> <staged-digital-object-set> < <csv-file>
            |
            |Options:
            |""".stripMargin)

  val mayNotExist = singleArgConverter[File](conv = new File(_))
  val shouldExist = singleArgConverter[File](conv = {f =>
    if (!new File(f).isDirectory) {
      log.error(s"$f is not an existing directory")
      throw new IllegalArgumentException()
    }
    new File(f)
  })

  val bag = trailArg[File](
    name = "EASY-bag",
    descr = "Bag with extra metadata for EASY to be staged for ingest into Fedora",
    required = true)(shouldExist)
  val sdoSet = trailArg[File](
    name = "staged-digital-object-set",
    descr = "An existing Staged Digital Object directory",
    required = true)(shouldExist)
}
