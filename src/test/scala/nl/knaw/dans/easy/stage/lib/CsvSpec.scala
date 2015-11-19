package nl.knaw.dans.easy.stage.lib

import java.io.{ByteArrayInputStream, FileInputStream}

import nl.knaw.dans.easy.stage.fileitem.{FileItemSettings, FileItemConf}
import org.scalatest.{FlatSpec, Matchers}

class CsvSpec extends FlatSpec with Matchers {

  private val commandLineArgs = "target/test/sdo-set".split(" ")
  private val conf = new FileItemConf(commandLineArgs)
  private val requiredHeaders = conf.longOptionNames.map(_.toUpperCase)

  "csv file with too few headers" should "fail" in {
    val in = new ByteArrayInputStream (
      "TITLE,DESCRIPTION,FORMAT,CREATED,FILE,MD5,DATASET-ID,xxx,STAGED-DIGITAL-OBJECT-SET"
        .stripMargin.getBytes)
    the[Exception] thrownBy CSV(in, requiredHeaders).get should
      have message "Missing columns: FILE-PATH, IDENTIFIER"
  }

  "proper csv file" should "render FileItemSettings" in {
    // NB: if a FILE does not exists, FileItemConf exits and thus terminates the full test suite
    val in = new ByteArrayInputStream(
      """xx,DATASET-ID,aa,FILE-PATH,FILE,MD5,FORMAT,IDENTIFIER,TITLE,DESCRIPTION,CREATED
        |yy,easy-dataset:1,aa,path/to/dir
        |
        |some comment in an ignored column if you like
        |and empty lines wherever you want
        |
        |zz,easy-dataset:1,aa,path/to/dir/qs.hs,src/test/resources/example-bag/data/quicksort.hs,eb0c93c460fac5cca0fd789d17c52daa,
        |
      """.stripMargin.getBytes)
    val trailArgs = Seq(conf.sdoSetDir.apply().toString)
    val (ignored, csv) = CSV(in, requiredHeaders).get
    val settingses = csv.getRows.map(options => FileItemSettings(options ++ trailArgs)).toArray
    ignored shouldBe List("xx","aa")
    settingses should have length 2
    settingses(0).filePath.get.toString shouldBe "path/to/dir"
    settingses(0).file.isDefined shouldBe false
    settingses(1).filePath.get.toString shouldBe "path/to/dir/qs.hs"
    settingses(1).file.get.getName shouldBe "quicksort.hs"
  }

  "sample.csv" should "render one or more FileItemSettings" in {
    val trailArgs = Seq(conf.sdoSetDir.apply().toString)
    val exampleCSV = new FileInputStream("src/test/resources/example.csv")
    val (ignored, csv) = CSV(exampleCSV, requiredHeaders).get
    ignored shouldBe List("comment")
    csv.getRows.map(options => FileItemSettings(options ++ trailArgs)) should not have length(0)
  }
}
