package nl.knaw.dans.easy.stage.dataset

import java.io.File
import java.net.UnknownHostException
import java.nio.file.Files

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{ Schema, SchemaFactory }
import nl.knaw.dans.easy.stage.{ CanConnectFixture, Settings }
import org.apache.commons.io.FileUtils
import org.joda.time.{ DateTime, DateTimeUtils }
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Inside, Matchers }

import scala.util.{ Failure, Try }
import scala.xml.SAXParseException

class MdFixture extends FlatSpec with Matchers with Inside with CanConnectFixture with BeforeAndAfterEach {

  val testDir = new File(s"target/test/${ getClass.getSimpleName }")
  val sdoSetDir = new File(s"$testDir/sdoSet")

  val nowYMD = "2018-03-22"
  val now = s"${ nowYMD }T21:43:01.576"
  val nowUTC = s"${ nowYMD }T20:43:01Z"
  /** Causes DateTime.now() to return a predefined value. */
  DateTimeUtils.setCurrentMillisFixed(new DateTime(nowUTC).getMillis)

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
    if (Files.exists(testDir.toPath)) {
      FileUtils.deleteDirectory(testDir)
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
