package nl.knaw.dans.easy.stage

import java.io.{ByteArrayOutputStream, File}

import nl.knaw.dans.easy.stage.CustomMatchers._
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

// TODO fix NullPointerException when subclassing AbstractConfSpec
class FileItemConfSpec extends FlatSpec with Matchers {

  val commandLineArgs = "-".split(" ")
  private val conf = new FileItemConf(commandLineArgs)

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