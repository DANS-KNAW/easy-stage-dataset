package nl.knaw.dans.easy.stage

import org.rogach.scallop.ScallopConf

class Conf(args: Seq[String]) extends ScallopConf(args) {
  printedName = "easy-ingest"
  version(s"$printedName ${Version()}")
  banner("""
           |Stage a dataset in EASY-BagIt format for ingest into an EASY Fedora Commons 3.x Repository.
           |
           |Usage: easy-stage-dataset [-u <user> -p <password>] [-f <fcrepo-server>][-i]
           |    [<staged-digital-object>... | <staged-digital-object-set>]
           |Options:
           |""".stripMargin)



}
