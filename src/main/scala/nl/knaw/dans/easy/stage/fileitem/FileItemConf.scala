package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.util.UUID
import nl.knaw.dans.easy.stage.lib.Version
import org.joda.time.DateTime
import org.rogach.scallop.{ScallopConf, ValueConverter, singleArgConverter}
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
            | $printedName <staged-digital-object-set> < <utf8-csv-file>
            |
            | The CSV file should have one column for each option, it may have more columns.
            |
            |Options:
            |""".stripMargin)


  implicit val dateTimeConv: ValueConverter[DateTime] = singleArgConverter[DateTime](conv = DateTime.parse)
  val mayNotExist = singleArgConverter[File](conv = new File(_))
  val shouldBeFile = singleArgConverter[File](conv = {f =>
    if (!new File(f).isFile) {
      log.error(s"$f is not an existing directory")
      throw new IllegalArgumentException()
    }
    new File(f)
  })

  val filePath = opt[File](
    name = "file-path", short = 'p',
    descr = "the path that the file should get in the dataset"
    )(mayNotExist)
  val identifier = opt[String](
    name = "identifier", short = 'u',
    descr = "dcterms property")
  val title = opt[List[String]](
    name = "title",
    descr = "dcterms property title and optional alternatives")
  val description = opt[String](
    name = "description",
    descr = "dcterms property description")
  val format = opt[String](
    name = "format", noshort = true,
    descr = "dcterms property format, the mime type of the file")
  val created = opt[DateTime](
    name = "created",
    descr = "dcterms property, date-time when the file was created")
  val file = opt[File](
    name = "file",
    descr = "File to stage for ingest into Fedora, if omitted a folder is staged"
    )(shouldBeFile)
  val md5 = opt[String](
    name = "md5",
    descr = "MD5 checksum of the file to stage")
  val datasetId = opt[String](
    name = "dataset-id", short = 'i',
    descr = "id of the dataset in Fedora that should receive the file to stage " +
     "if omitted the csf-file is read")
  dependsOnAll(file, List(filePath,md5,datasetId))
  dependsOnAll(created, List(filePath,md5,datasetId))
  dependsOnAll(format, List(filePath,md5,datasetId))
  dependsOnAll(description, List(filePath,md5,datasetId))
  dependsOnAll(title, List(filePath,md5,datasetId))
  dependsOnAll(identifier, List(filePath,md5,datasetId))

  val sdoSetDir = trailArg[File](
    name = "staged-digital-object-set",
    descr = "The resulting Staged Digital Object directory (will be created if it does not exist)",
    required = true)(mayNotExist)
}
