package nl.knaw.dans.easy.stage.lib

import java.io.{ByteArrayInputStream, FileInputStream}

import nl.knaw.dans.easy.stage.CustomMatchers._
import nl.knaw.dans.easy.stage.fileitem.{FileItemConf, FileItemSettings}
import org.scalatest.{Matchers, FlatSpec}

import scala.util.{Success, Failure}

class CsvSpec extends FlatSpec with Matchers {

  val commandLineArgs = "target/test/sdo-set".split(" ")
  private val conf = new FileItemConf(commandLineArgs)

  "csv file with too few headers" should "fail" in {
    val in = new ByteArrayInputStream (
      "TITLE,DESCRIPTION,FORMAT,CREATED,FILE,MD5,DATASET-ID,xxx,STAGED-DIGITAL-OBJECT-SET"
        .stripMargin.getBytes)
    // TODO verify warning "Ignoring columns: xxx, STAGED-DIGITAL-OBJECT-SET"
    // as the logger sits on an object we can't use the trick of
    // https://github.com/DANS-KNAW/easy-update-solr-index/pull/12#issuecomment-146122207

    CSV(in, conf) should failWithMessage("Missing columns: FILE-PATH, IDENTIFIER")
  }

  "proper csv file" should "render FileItemSettings" in {
    // NB: if a FILE does not exists, FileItemConf exits and thus terminates the full test suite
    val in = new ByteArrayInputStream(
      """xx,DATASET-ID,FILE-PATH,FILE,MD5,FORMAT,IDENTIFIER,TITLE,DESCRIPTION,CREATED
        |yy,easy-dataset:1,path/to/dir
        |
        |some comment in an ignored column if you like
        |and empty lines wherever you want
        |
        |zz,easy-dataset:1,path/to/dir/qs.hs,src/test/resources/example-bag/data/quicksort.hs,eb0c93c460fac5cca0fd789d17c52daa,
        |
      """.stripMargin.getBytes)
    val records = CSV(in, conf).get.map(
      args => FileItemSettings(new FileItemConf(args ++ List(conf.sdoSetDir.apply().toString)))
    ).toArray
    records should have length 2
    records(0).filePath.toString shouldBe "path/to/dir"
    records(0).file.isDefined shouldBe false
    records(1).filePath.toString shouldBe "path/to/dir/qs.hs"
    records(1).file.get.getName shouldBe "quicksort.hs"
  }

  "sample.csv" should "render one or more FileItemSettings" in {
    val exampleCSV = new FileInputStream("src/test/resources/example.csv")
    CSV(exampleCSV, conf).get should not have length (0)
  }
}
