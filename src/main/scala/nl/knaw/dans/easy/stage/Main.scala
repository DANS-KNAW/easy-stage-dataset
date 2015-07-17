package nl.knaw.dans.easy.stage

import java.io.{PrintWriter, File}
import java.nio.file.Paths

import scala.util.Try

object Main {

  case class Settings(bagitDir: File, sdoSetDir: File)

  def main(args: Array[String]) {
    implicit val s = Settings(
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
        createFileDO(child)
      } else if (child.isDirectory) {
        createDirDO(child)
        createDigitalObjects(child)
      }
    })
  }

  def createFileDO(file: File)(implicit s: Settings): Try[Unit] = Try {
    val sdoDir = getDODir(file)
    println("file DO> " + sdoDir.getPath)
    sdoDir.mkdir()

    createFileFOXML(sdoDir)
  }

  def createDirDO(dir: File)(implicit s: Settings): Try[Unit] = Try {
    val sdoDir = getDODir(dir)
    println("dir DO> " + sdoDir.getPath)
    sdoDir.mkdir()
  }

  def createFileFOXML(sdoDir: File): Try[Unit] = Try {
    val pw = new PrintWriter(Paths.get(sdoDir.getPath, "fo.xml").toFile)
    pw.write(getFileFOXML())
    pw.close()
  }

  def getFileFOXML(): String = {
//    <?xml version="1.0" encoding="UTF-8"?>
    <foxml:digitalObject VERSION="1.1"
                         xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd">
      <foxml:objectProperties>
        <foxml:property NAME="info:fedora/fedora-system:def/model#state" VALUE="Active"/>
      </foxml:objectProperties>
      <foxml:datastream ID="DC" STATE="A" CONTROL_GROUP="X" VERSIONABLE="true">
        <foxml:datastreamVersion ID="DC1.0" LABEL="Dublin Core Record for this object"
                                 MIMETYPE="text/xml" FORMAT_URI="http://www.openarchives.org/OAI/2.0/oai_dc/">
          <foxml:xmlContent>
            <oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                       xmlns:dc="http://purl.org/dc/elements/1.1/"
                       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                       xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
              <dc:identifier>ingest-test:1</dc:identifier>
              <dc:description>An object ingested by posting a FOXML document to the REST API.</dc:description>
            </oai_dc:dc>
          </foxml:xmlContent>
        </foxml:datastreamVersion>
      </foxml:datastream>
    </foxml:digitalObject>.mkString
  }

  def getDODir(fileOrDir: File)(implicit s: Settings): File = {
    val sdoName = fileOrDir.getPath.replace(s.bagitDir.getPath, "").replace("/", "_").replace(".", "_") match {
      case name if name.startsWith("_") => name.tail
      case name => name
    }
    Paths.get(s.sdoSetDir.getPath, sdoName).toFile
  }
}
