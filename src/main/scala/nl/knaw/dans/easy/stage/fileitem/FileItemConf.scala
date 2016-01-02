package nl.knaw.dans.easy.stage.fileitem

import java.io.File

import nl.knaw.dans.easy.stage.lib.Version
import org.joda.time.DateTime
import org.rogach.scallop.{TrailingArgsOption, ScallopConf, ValueConverter, singleArgConverter}
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


  implicit val dateTimeConv: ValueConverter[DateTime] = singleArgConverter[DateTime](conv = DateTime.parse)
  val mayNotExist = singleArgConverter[File](conv = new File(_))
  val shouldBeFile = singleArgConverter[File](conv = {f =>
    if (!new File(f).isFile) throw new IllegalArgumentException(s"$f is not an existing file")
    new File(f)
  })

  val pathInDataset = opt[File](
    name = "path-in-dataset", short = 'p',
    descr = "the path that the file or folder should get in the dataset")(mayNotExist)
  val format = opt[String](
    name = "format", noshort = true,
    descr = "dcterms property format, the mime type of the file")
  val pathInStorage = opt[File](
    name = "path-in-storage",
    descr = "Path of the file in storage, relative to the storage-base-url, if omitted a folder is staged")(mayNotExist)
  val datasetId = opt[String](
    name = "dataset-id", short = 'i',
    descr = "id of the dataset in Fedora that should receive the file to stage (requires file-path). " +
     "If omitted the trailing argument csf-file is required")
  codependent(datasetId,pathInDataset)
  codependent(pathInStorage,format)
  dependsOnAll(pathInStorage, List(datasetId))

  val csvFile = trailArg[File](
    name = "csv-file",
    descr = "a comma separated file with one column for each option " +
     "(additional columns are ignored) and one set of options per line",
    required = false)(shouldBeFile)
  val sdoSetDir = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)(mayNotExist)

  val longOptionNames = builder.opts.filter(!_.isInstanceOf[TrailingArgsOption]).map(_.name)

  override def toString = builder.args.mkString(", ")
}
