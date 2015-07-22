package nl.knaw.dans.easy.stage

import java.io.{File, PrintWriter}
import java.nio.file.Paths

import org.apache.commons.io.FileUtils
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.Try
import scala.xml.XML

object Main {

  private val CONFIG_FILENAME = "cfg.json"
  private val FOXML_FILENAME = "fo.xml"

  case class Settings(ownerId: String, bagStorageLocation: String, bagitDir: File, sdoSetDir: File)

  def main(args: Array[String]) {

    implicit val s = Settings(
      ownerId = "georgi",
      bagStorageLocation = "http://localhost/bags",
      bagitDir = new File("test-resources/example-bag"),
      sdoSetDir = new File("out/sdoSetDir"))

    val dataDir = s.bagitDir.listFiles.find(_.getName == "data")
      .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))

    createDigitalObjects(dataDir)
  }

  def createDigitalObjects(dataDir: File)(implicit s: Settings): Try[Unit] = Try {
    val children = dataDir.listFiles()
    children.foreach(child => {
      if (child.isFile) {
        createFileSDO(child)
      } else if (child.isDirectory) {
        createDirSDO(child)
        createDigitalObjects(child)
      }
    })
  }

  def createFileSDO(file: File)(implicit s: Settings): Try[Unit] = Try {
    val sdoDir = getSDODir(file)
    sdoDir.mkdir()
    FileUtils.copyFileToDirectory(file, sdoDir)
    val relativePath = file.getPath.replaceFirst(s.bagitDir.getPath, "").substring(1)
    val mimeType = readMimeType(relativePath)
    createFileJsonCfg(file.getName, s"${s.bagStorageLocation}/$relativePath", mimeType, sdoDir)
    createFileFOXML(file.getName, s.ownerId, mimeType, sdoDir)
  }

  def createDirSDO(dir: File)(implicit s: Settings): Try[Unit] = Try {
    val sdoDir = getSDODir(dir)
    sdoDir.mkdir()
  }

  def createFileJsonCfg(filename: String, fileLocation: String, mimeType: String, sdoDir: File)(implicit s: Settings): Try[Unit] = Try {
    val pw = new PrintWriter(Paths.get(sdoDir.getPath, CONFIG_FILENAME).toFile)
    val sdoCfg =
      ("namespace" -> "easy-file") ~
      ("datastreams" -> List(
        ("dsLocation" -> fileLocation ) ~
        ("dsID" -> "REMOTE_BYTES") ~
        ("controlGroup" -> "R") ~
        ("mimeType" -> mimeType)
      ))
    pw.write(pretty(render(sdoCfg)))
    pw.close()
  }

  def createFileFOXML(filename: String, ownerId: String, mimeType: String, sdoDir: File)(implicit s: Settings): Try[Unit] = Try {
    val pw = new PrintWriter(Paths.get(sdoDir.getPath, FOXML_FILENAME).toFile)
    pw.write(getFileFOXML(filename, ownerId, mimeType))
    pw.close()
  }

  def getFileFOXML(label: String, ownerId: String, mimeType: String): String = {
//    <?xml version="1.0" encoding="UTF-8"?>
    <foxml:digitalObject VERSION="1.1"
                         xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd">
      <foxml:objectProperties>
        <foxml:property NAME="info:fedora/fedora-system:def/model#state" VALUE="Active"/>
        <foxml:property NAME="info:fedora/fedora-system:def/model#label" VALUE={label}/>
        <foxml:property NAME="info:fedora/fedora-system:def/model#ownerId" VALUE={ownerId}/>
      </foxml:objectProperties>
      <foxml:datastream ID="DC" STATE="A" CONTROL_GROUP="X" VERSIONABLE="true">
        <foxml:datastreamVersion ID="DC1.0" LABEL="Dublin Core Record for this object"
                                 MIMETYPE="text/xml" FORMAT_URI="http://www.openarchives.org/OAI/2.0/oai_dc/">
          <foxml:xmlContent>
            <oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                       xmlns:dc="http://purl.org/dc/elements/1.1/"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
              <dc:title>{label}</dc:title>
              <dc:type>{mimeType}</dc:type>
            </oai_dc:dc>
          </foxml:xmlContent>
        </foxml:datastreamVersion>
      </foxml:datastream>
    </foxml:digitalObject>.mkString
  }

  def readMimeType(filePath: String)(implicit s: Settings): String = {
    val filesMetadata = new File(s.bagitDir, "metadata/files.xml")
    if (!filesMetadata.exists) {
      throw new RuntimeException("Unable to find `metadata/files.xml` in bag.")
    }
    val mimes = for {
      file <- XML.loadFile(filesMetadata) \\ "files" \ "file"
      if (file \ "@filepath").text == filePath
      mime <- file \ "format"
    } yield mime
    if (mimes.size != 1)
      throw new RuntimeException(s"Filepath [$filePath] doesn't exist in files.xml, or isn't unique.")
    mimes(0).text
  }

  def getSDODir(fileOrDir: File)(implicit s: Settings): File = {
    val sdoName = fileOrDir.getPath.replace(s.bagitDir.getPath, "").replace("/", "_").replace(".", "_") match {
      case name if name.startsWith("_") => name.tail
      case name => name
    }
    Paths.get(s.sdoSetDir.getPath, sdoName).toFile
  }
}
