package nl.knaw.dans.easy.stage.dataset

import java.io.File
import java.net.UnknownHostException
import java.nio.file.{ Files, Path }

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{ Schema, SchemaFactory }
import nl.knaw.dans.easy.stage.{ CanConnectFixture, Settings }
import org.apache.commons.io.FileUtils
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Inside, Matchers }

import scala.util.{ Failure, Try }
import scala.xml.SAXParseException

class MdFixture extends FlatSpec with Matchers with Inside with CanConnectFixture with BeforeAndAfterEach {

  val sdoSetDir = new File("target/test/EmdSpec/sdoSet")
  val depositorInfoDir: Path = sdoSetDir.toPath.resolve("metadata/depositor-info")

  def newSettings(bagitDir: File): Settings = {
    new Settings(
      ownerId = "",
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      state = "DRAFT",
      archive = "EASY",
      disciplines = Map.empty,
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "",
      licenses = Map.empty,
      stageDatasetVersion = "test",
    )
  }

  override def beforeEach(): Unit = {
    if (Files.exists(sdoSetDir.toPath)) {
      FileUtils.deleteDirectory(sdoSetDir)
    }
  }

  def loadSchema(schema: String): Try[Schema] = {
    Try {
      SchemaFactory
        .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(Array(new StreamSource(schema)).toArray[Source])
    }
  }

  def isAvailable(schema: Try[Any]): Boolean = {
    schema match {
      case Failure(e: SAXParseException) if e.getCause.isInstanceOf[UnknownHostException] => false
      case Failure(e: SAXParseException) if e.getMessage.contains("Cannot resolve") =>
        println("Probably an offline third party schema: " + e.getMessage)
        false
      case _ => true
    }
  }
}
