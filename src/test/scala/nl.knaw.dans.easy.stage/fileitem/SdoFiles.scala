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
package nl.knaw.dans.easy.stage.fileitem

import java.io.File

import nl.knaw.dans.easy.stage.lib.Util.loadXML

/**
 * Gets filenames of and SDO set or essential contents of SDO files
 * with as little knowledge as possible yet insensitive to
 * varying whitespace, order of elements and namespace prefixes.
 */
object SdoFiles {

  /** Gets (label,text) of dc elements and (name,value)-attributes of object properties,
   * labels are prefixed with "dc_" and names are prefixed with "prop_". */
  def readDatastreamFoxml(file: String): Set[(String, String)] = {
    val xml = loadXML(new File(file))
    (xml \\ "dc" \ "_")
      .map(node => s"dc_${ node.label }" -> node.text)
      .toSet ++
      (xml \\ "property")
        .map(node => s"prop_${ (node \ "@NAME").toString().replaceAll(".*#", "") }" -> (node \ "@VALUE").toString()).toSet
  }
}
