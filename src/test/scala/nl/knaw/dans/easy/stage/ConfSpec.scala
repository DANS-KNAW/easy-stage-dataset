package nl.knaw.dans.easy.stage

import java.io.{ByteArrayOutputStream, File}
import java.lang.System.clearProperty

import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.{Matchers, FlatSpec}

import nl.knaw.dans.easy.stage.CustomMatchers._
import scala.collection.JavaConverters._

class ConfSpec extends FlatSpec with Matchers {

  val commandLineArgs = "-t 2015 -u urn -d doi -b . -s -".split(" ")
  private val conf = new Conf(commandLineArgs)
  val helpInfo = {
    val mockedStdOut = new ByteArrayOutputStream()
    Console.withOut(mockedStdOut) {
      conf.printHelp()
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

  "first banner line" should "be part of README.md and pom.xml" in {
    val description = helpInfo.split("\n")(1)
    new File("README.md") should containTrimmed(description)
    new File("pom.xml") should containTrimmed(description)
  }

  "distributed default properties" should "be valid options" in {
    val optKeys = conf.builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default.") )

    propKeys.foreach(key => optKeys should contain (key.replace("default.","")) )
  }
}