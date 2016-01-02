package nl.knaw.dans.easy.stage.lib

import java.io.{ByteArrayInputStream, FileInputStream}

import nl.knaw.dans.easy.stage.fileitem.{FileItemSettings, FileItemConf}
import org.scalatest.{FlatSpec, Matchers}

class CsvSpec extends FlatSpec with Matchers {

  private val commandLineArgs = "target/test/sdo-set".split(" ")
  private val conf = new FileItemConf(commandLineArgs)

  "apply" should "fail with too few headers in the input" in {
    val in = new ByteArrayInputStream (
      "FORMAT,DATASET-ID,xxx,STAGED-DIGITAL-OBJECT-SET"
        .stripMargin.getBytes)
    the[Exception] thrownBy CSV(in, conf.longOptionNames).get should
      have message "Missing columns: PATH-IN-DATASET, PATH-IN-STORAGE"
  }

  it should "fail with uppercase in any of the required headers" in {
    val in = new ByteArrayInputStream ("".stripMargin.getBytes)
    (the[Exception] thrownBy CSV(in, Seq("ABc", "def")).get).getMessage should include ("not supported")
  }
}
