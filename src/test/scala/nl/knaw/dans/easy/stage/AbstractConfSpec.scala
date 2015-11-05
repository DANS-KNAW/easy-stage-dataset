package nl.knaw.dans.easy.stage

import java.io.{File, ByteArrayOutputStream}

import nl.knaw.dans.easy.stage.CustomMatchers._
import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import org.scalatest.{Matchers, FlatSpec}
import scala.collection.JavaConverters._

abstract class AbstractConfSpec extends FlatSpec with Matchers {

  def getConf: ScallopConf

  val helpInfo = {
    val mockedStdOut = new ByteArrayOutputStream()
    Console.withOut(mockedStdOut) {
      getConf.printHelp()
    }
    mockedStdOut.toString
  }

  "options in help info" should "be part of README.md" in {
    val options = helpInfo.split("Options:")(1)
    new File("README.md") should containTrimmed(options)
  }

  "synopsis in help info" should "be part of README.md" in {
    val synopsis = helpInfo.split("Options:")(0).split("Usage:")(1)
    new File("README.md") should containTrimmed(synopsis)
  }

  "distributed default properties" should "be valid options" in {
    val optKeys = getConf.builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default.") )

    propKeys.foreach(key => optKeys should contain (key.replace("default.","")) )
  }

}
