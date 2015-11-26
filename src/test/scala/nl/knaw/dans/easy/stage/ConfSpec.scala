package nl.knaw.dans.easy.stage

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.clearProperty

import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import org.scalatest.{Matchers, FlatSpec}

import nl.knaw.dans.easy.stage.CustomMatchers._
import scala.collection.JavaConverters._

class ConfSpec extends AbstractConfSpec {

  override def getConf: ScallopConf = new Conf("-t 2015 -u urn -d doi . -".split(" "))

  "first banner line" should "be part of README.md and pom.xml" in {
    val description = helpInfo.split("\n")(1)
    new File("README.md") should containTrimmed(description)
    new File("pom.xml") should containTrimmed(description)
  }
}