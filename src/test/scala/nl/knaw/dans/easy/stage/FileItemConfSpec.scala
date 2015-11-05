package nl.knaw.dans.easy.stage

import java.io.{ByteArrayOutputStream, File}

import nl.knaw.dans.easy.stage.CustomMatchers._
import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.JavaConverters._

class FileItemConfSpec extends AbstractConfSpec {

  override def getConf: ScallopConf = new FileItemConf("-".split(" "))
}