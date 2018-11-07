/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.lib

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import nl.knaw.dans.pf.language.emd.EasyMetadata

import scala.xml.{ Elem, NodeSeq, XML }

object FOXML extends DebugEnhancedLogging {
  private val MAX_LABEL_LENGTH = 255

  def getDatasetFOXML(ownerId: String, emd: EasyMetadata): String = {
    trace(ownerId)
    /*
     * NOTE: DO NOT USE THE asXMLString method. It uses the platform's default charset, which can lead to unexpected
     * problems with the output.
     *
     * See https://drivenbydata.atlassian.net/browse/EASY-984
     */
    val dc = XML.load(emd.getDublinCoreMetadata.asXMLInputStream())
    getFOXML(emd.getPreferredTitle, ownerId, dc).toString()
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

  private def getFOXML(label: String, ownerId: String, dcElems: NodeSeq, contentDigest: NodeSeq = NodeSeq.Empty): Elem = {
    <foxml:digitalObject VERSION="1.1"
                         xmlns:foxml="info:fedora/fedora-system:def/foxml#"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="info:fedora/fedora-system:def/foxml# http://www.fedora.info/definitions/1/0/foxml1-1.xsd">
      <foxml:objectProperties>
        <foxml:property NAME="info:fedora/fedora-system:def/model#state" VALUE="Active" />
        <foxml:property NAME="info:fedora/fedora-system:def/model#label" VALUE={label.substring(0, Math.min(label.length, MAX_LABEL_LENGTH))} />
        <foxml:property NAME="info:fedora/fedora-system:def/model#ownerId" VALUE={ownerId} />
      </foxml:objectProperties>
      <foxml:datastream ID="DC" STATE="A" CONTROL_GROUP="X" VERSIONABLE="true">
        <foxml:datastreamVersion ID="DC1.0" LABEL="Dublin Core Record"
                                 MIMETYPE="text/xml" FORMAT_URI="http://www.openarchives.org/OAI/2.0/oai_dc/">
          {contentDigest}
          <foxml:xmlContent>
            {dcElems}
          </foxml:xmlContent>
        </foxml:datastreamVersion>
      </foxml:datastream>
    </foxml:digitalObject>
  }
}
