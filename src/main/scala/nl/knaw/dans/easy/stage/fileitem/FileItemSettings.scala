/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.fileitem.FileItemSettings._
import nl.knaw.dans.easy.stage.lib.Fedora
import nl.knaw.dans.easy.stage.lib.Props.props

import scala.util.Try

case class FileItemSettings (sdoSetDir: Option[File],
                             datasetId: Option[String],
                             datastreamLocation: Option[URL],
                             unsetUrl: URL = new URL(props.getString("redirect-unset-url")),
                             size: Option[Long],
                             ownerId: String = props.getString("owner"),
                             pathInDataset: Option[File],
                             title:  Option[String] = None,
                             format: Option[String] = None,

                             // as in SDO/*/EASY_FILE_METADATA
                             creatorRole: String = defaultCreatorRole,
                             visibleTo: String = defaultVisibleTo,
                             accessibleTo: String = defaultAccessibleTo,
                             fedora: Fedora = Fedora,
                             easyFilesAndFolders: EasyFilesAndFolders = EasyFilesAndFolders,

                             subordinate: (String, String)) {
  if (!FileItemSettings.creatorRoles.contains(creatorRole))
    throw new Exception(s"illegal value for creatorRole, got $creatorRole")
  if (!FileItemSettings.accessCategories.contains(visibleTo))
    throw new Exception(s"illegal value for visibleTo, got $visibleTo")
  if (!FileItemSettings.accessCategories.contains(accessibleTo))
    throw new Exception(s"illegal value for accessibleTo, got $accessibleTo")
}

object FileItemSettings {
  val defaultCreatorRole = "DEPOSITOR"
  val defaultVisibleTo = "ANONYMOUS"
  val defaultAccessibleTo = "NONE"
  val accessCategories =  Array("ANONYMOUS", "KNOWN", "RESTRICTED_REQUEST", "RESTRICTED_GROUP", "NONE")
  val creatorRoles =  Array("ARCHIVIST", "DEPOSITOR")

  /** new file or folder for a new dataset */
  def apply(sdoSetDir: File,
            ownerId: String,
            pathInDataset: File,
            format: Option[String],
            title: Option[String],
            size: Option[Long]
           ) =
    // no need to catch exceptions thrown by the constructor as the defaults take care of valid values
    new FileItemSettings(
      sdoSetDir = Some(sdoSetDir),
      datasetId = None,
      datastreamLocation = None,
      size = size,
      ownerId = ownerId,
      pathInDataset = Some(pathInDataset),
      format = format,
      title = title,
      subordinate = "objectSDO" -> "dataset"
    )

  /** new file or folder for an existing dataset */
  def apply(conf: FileItemConf) =
    // no need to catch exceptions thrown by the constructor as FileItemConf performs the same checks
    new FileItemSettings(
      sdoSetDir = conf.sdoSetDir.get,
      datastreamLocation = conf.dsLocation.get,
      size = conf.size.get,
      accessibleTo = conf.accessibleTo(),
      visibleTo = conf.visibleTo(),
      creatorRole = conf.creatorRole(),
      ownerId = conf.ownerId.get.map(_.trim).filter(_.nonEmpty).getOrElse(props.getString("owner")),
      datasetId = conf.datasetId.get,
      pathInDataset = conf.pathInDataset.get,
      format = conf.format.get,
      subordinate = "object" -> s"info:fedora/${conf.datasetId()}"
    ) {
      override def toString = conf.toString
    }
}
