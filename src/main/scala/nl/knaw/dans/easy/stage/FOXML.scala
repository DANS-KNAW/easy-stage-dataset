package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.Util._
import nl.knaw.dans.pf.language.emd.EasyMetadata

import scala.util.Try
import scala.xml.{Elem, NodeSeq, XML}

object FOXML {

  def create(sdoDir: File, foxml: => String)(implicit s: SharedSettings): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, FOXML_FILENAME), foxml)

  def getDatasetFOXML(ownerId: String, emd: EasyMetadata): String = {
    val dc = XML.loadString(emd.getDublinCoreMetadata.asXMLString())
    getFOXML(emd.getPreferredTitle, ownerId, dc).toString
  }

  def getFileFOXML(label: String, ownerId: String, mimeType: String): String = {
    val dc =
      <oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                 xmlns:dc="http://purl.org/dc/elements/1.1/"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
        <dc:title>{label}</dc:title><dc:type>{mimeType}</dc:type>
      </oai_dc:dc>
    getFOXML(label, ownerId, dc).mkString
  }

  def getDirFOXML(label: String, ownerId: String): String = {
    val dc =
      <oai_dc:dc xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
                 xmlns:dc="http://purl.org/dc/elements/1.1/"
                 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                 xsi:schemaLocation="http://www.openarchives.org/OAI/2.0/oai_dc/ http://www.openarchives.org/OAI/2.0/oai_dc.xsd">
        <dc:title>{label}</dc:title>
      </oai_dc:dc>
    getFOXML(label, ownerId, dc).mkString
  }

  private def getFOXML(label: String, ownerId: String, dcElems: NodeSeq): Elem = {
    //    <?xml version="1.0" encoding="UTF-8"?>
    <foxml:digitalObject VERSION="1.1"
                         xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd">
      <foxml:objectProperties>
        <foxml:property NAME="info:fedora/fedora-system:def/model#state" VALUE="Active" />
        <foxml:property NAME="info:fedora/fedora-system:def/model#label" VALUE={label} />
        <foxml:property NAME="info:fedora/fedora-system:def/model#ownerId" VALUE={ownerId} />
      </foxml:objectProperties>
      <foxml:datastream ID="DC" STATE="A" CONTROL_GROUP="X" VERSIONABLE="true">
        <foxml:datastreamVersion ID="DC1.0" LABEL="Dublin Core Record"
                                 MIMETYPE="text/xml" FORMAT_URI="http://www.openarchives.org/OAI/2.0/oai_dc/">
          <foxml:xmlContent>
            {dcElems}
          </foxml:xmlContent>
        </foxml:datastreamVersion>
      </foxml:datastream>
    </foxml:digitalObject>
  }
}
