package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Util._
import nl.knaw.dans.pf.language.ddm.api.Ddm2EmdCrosswalk
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.binding.EmdMarshaller
import org.apache.commons.io.FileUtils
import org.json4s.JsonDSL._
import org.json4s.native.JsonMethods._

import scala.util.{Failure, Success, Try}
import scala.xml.{Elem, NodeSeq, XML}

object Main {

  private val CONFIG_FILENAME = "cfg.json"
  private val FOXML_FILENAME = "fo.xml"
  private val DATASET_SDO = "dataset"

  case class Settings(ownerId: String, bagStorageLocation: String, bagitDir: File, sdoSetDir: File)

  def main(args: Array[String]) {

    implicit val s = Settings(
      ownerId = "georgi",
      bagStorageLocation = "http://localhost/bags",
      bagitDir = new File("test-resources/example-bag"),
      sdoSetDir = new File("out/sdoSetDir"))

    val dataDir = s.bagitDir.listFiles.find(_.getName == "data")
      .getOrElse(throw new RuntimeException("Bag doesn't contain data directory."))

    createDatasetSDO().flatMap(_ => createSDOs(dataDir, DATASET_SDO)).get
  }

  def createDatasetSDO()(implicit s: Settings): Try[Unit] =
    for {
      sdoDir <- mkdirSafe(new File(s.sdoSetDir, DATASET_SDO))
      _ <- createAMD(sdoDir)
      _ <- createEMDAndDC(sdoDir)
      _ <- createPRSQL(sdoDir)
    } yield ()

  def createAMD(sdoDir: File)(implicit s: Settings): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, "AMD"), AMD(s.ownerId, "2015-07-09T10:38:24.570+02:00").toString())

  def createEMDAndDC(sdoDir: File)(implicit s: Settings): Try[Unit] = {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists()) {
      return Failure(new RuntimeException(s"Couldn't find ${sdoDir.getName}/metadata/dataset.xml"))
    }
    for {
      emd <- getEasyMetadata(ddm)
      _   <- writeToFile(new File(sdoDir, "EMD"), new EmdMarshaller(emd).getXmlString)
      _   <- writeToFile(new File(sdoDir, "DC"), emd.getDublinCoreMetadata.asXMLString())
    } yield ()
  }

  def getEasyMetadata(ddm: File): Try[EasyMetadata] =
    try {
      val crosswalk = new Ddm2EmdCrosswalk()
      val emd = crosswalk.createFrom(ddm)
      if (emd == null)
        Failure(new RuntimeException(s"${crosswalk.getXmlErrorHandler.getMessages}"))
      else
        Success(emd)
    } catch {
      case t: Throwable => Failure(t)
    }

  def createPRSQL(sdoDir:File): Try[Unit] = {
    val prsql =
      <psl:permissionSequenceList xmlns:psl="http://easy.dans.knaw.nl/easy/permission-sequence-list/">
        <sequences></sequences>
      </psl:permissionSequenceList>.toString()
    writeToFile(new File(sdoDir.getPath, "PRSQL"), prsql)
  }

  def createSDOs(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    def visit(child: File): Try[Unit] =
      if (child.isFile)
        createFileSDO(child, parentSDO)
      else if (child.isDirectory)
        createDirSDO(child, parentSDO).flatMap(_ => createSDOs(child, child.getName))
      else
        Failure(new RuntimeException(s"Unknown object encountered while traversing ${dir.getName}: ${child.getName}"))
    Try { dir.listFiles().toList }.flatMap(_.map(visit).allSuccess)
  }

  def createFileSDO(file: File, parentSDO: String)(implicit s: Settings): Try[Unit] = {
    val relativePath = file.getPath.replaceFirst(s.bagitDir.getPath, "").substring(1)
    for {
      sdoDir <- mkdirSafe(getSDODir(file))
      mime <- readMimeType(relativePath)
      _ = FileUtils.copyFileToDirectory(file, sdoDir)
      _ <- createFileJsonCfg(s"${s.bagStorageLocation}/$relativePath", mime, parentSDO, sdoDir)
      _ <- createFOXML(sdoDir, getFileFOXML(file.getName, s.ownerId, mime))
    } yield ()
  }

  def createDirSDO(dir: File, parentSDO: String)(implicit s: Settings): Try[Unit] =
    for {
      sdoDir <- mkdirSafe(getSDODir(dir))
      _ <- createDirJsonCfg(dir.getName, parentSDO, sdoDir)
      _ <- createFOXML(sdoDir, getDirFOXML(dir.getName, s.ownerId))
    } yield ()

  def createFileJsonCfg(fileLocation: String, mimeType: String, parentSDO: String, sdoDir: File): Try[Unit] = {
    val sdoCfg =
      ("namespace" -> "easy-file") ~
      ("datastreams" -> List(
        ("dsLocation" -> fileLocation ) ~
        ("dsID" -> "EASY_FILE") ~
        ("controlGroup" -> "R") ~
        ("mimeType" -> mimeType))) ~
      ("relations" -> List(
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isMemberOf") ~ ("objectSDO" -> parentSDO),
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isSubordinateTo") ~ ("objectSDO" -> DATASET_SDO),
        ("predicate" -> "info:fedora/fedora-system:def/model#") ~ ("object" -> "info:fedora/easy-model:EDM1FILE"),
        ("predicate" -> "info:fedora/fedora-system:def/model#") ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    writeToFile(new File(sdoDir.getPath, CONFIG_FILENAME), pretty(render(sdoCfg)))
  }

  def createDirJsonCfg(dirName: String, parentSDO: String, sdoDir: File): Try[Unit] = {
    val sdoCfg =
      ("namespace" -> "easy-folder") ~
      ("relations" -> List(
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isMemberOf") ~ ("objectSDO" -> parentSDO),
        ("predicate" -> "http://dans.knaw.nl/ontologies/relations#:isSubordinateTo") ~ ("objectSDO" -> DATASET_SDO),
        ("predicate" -> "info:fedora/fedora-system:def/model#hasModel") ~ ("object" -> "info:fedora/easy-model:EDM1FOLDER"),
        ("predicate" -> "info:fedora/fedora-system:def/model#hasModel") ~ ("object" -> "info:fedora/dans-container-item-v1")
      ))
    writeToFile(new File(sdoDir.getPath, CONFIG_FILENAME), pretty(render(sdoCfg)))
  }

  def createFOXML(sdoDir: File, getFOXML: => String)(implicit s: Settings): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, FOXML_FILENAME), getFOXML)

  def getFileFOXML(label: String, ownerId: String, mimeType: String): String = {
    val dc = <dc:title>{label}</dc:title><dc:type>{mimeType}</dc:type>
    getFOXML(label, ownerId, dc).mkString
  }

  def getDirFOXML(label: String, ownerId: String): String = {
    val dc = <dc:title>{label}</dc:title>
    getFOXML(label, ownerId, dc).mkString
  }

  def getFOXML(label: String, ownerId: String, dcElems: NodeSeq): Elem = {
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
              {dcElems}
            </oai_dc:dc>
          </foxml:xmlContent>
        </foxml:datastreamVersion>
      </foxml:datastream>
    </foxml:digitalObject>
  }

  def readMimeType(filePath: String)(implicit s: Settings): Try[String] = Try {
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
    new File(s.sdoSetDir.getPath, sdoName)
  }

  class CompositeException(throwables: Seq[Throwable]) extends RuntimeException(throwables.foldLeft("")((msg, t) => s"$msg\n${t.getMessage}"))

  implicit class TryExtensions[T](xs: Seq[Try[Unit]]) {
    def allSuccess: Try[Unit] =
      if (xs.exists(_.isFailure))
        Failure(new CompositeException(xs.collect { case Failure(e) => e }))
      else
        Success(Unit)
  }

}

