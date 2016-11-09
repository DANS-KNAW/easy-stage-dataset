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
import nl.knaw.dans.easy.stage.fileitem.FileAccessRights.UserCategory
import nl.knaw.dans.easy.stage.lib.Fedora
import nl.knaw.dans.easy.stage.lib.Props.props

case class FileItemSettings (sdoSetDir: Option[File],
                             file: Option[File] = None,
                             datasetId: Option[String],
                             datastreamLocation: Option[URL] = None,
                             unsetUrl: URL = new URL(props.getString("redirect-unset-url")),
                             size: Option[Long] = None,
                             ownerId: Option[String] = None,
                             pathInDataset: Option[File],
                             title:  Option[String] = None,
                             format: Option[String] = None,
                             sha1: Option[String] = None,

                             // as in SDO/*/EASY_FILE_METADATA
                             creatorRole: String = defaultCreatorRole,
                             visibleTo: UserCategory = FileAccessRights.NONE,
                             accessibleTo: UserCategory = FileAccessRights.ANONYMOUS,
                             fedora: Fedora = Fedora,
                             easyFilesAndFolders: EasyFilesAndFolders = EasyFilesAndFolders,

                             subordinate: (String, String) = "objectSDO" -> "dataset") {
  require(FileItemSettings.creatorRoles.contains(creatorRole), s"illegal value for creatorRole, got $creatorRole")
}

object FileItemSettings {
  val defaultFormat = "application/octet-stream"
  val defaultCreatorRole = "DEPOSITOR"
  val creatorRoles =  Array("ARCHIVIST", "DEPOSITOR")

  /** new file for a new dataset */
  def apply(sdoSetDir: File,
            file: Option[File],
            ownerId: String,
            pathInDataset: File,
            format: Option[String],
            sha1: Option[String],
            title: Option[String],
            size: Option[Long],
            visibleTo: UserCategory,
            accessibleTo: UserCategory
           ) =
    // no need to catch exceptions thrown by the constructor as the defaults take care of valid values
    new FileItemSettings(
      sdoSetDir = Some(sdoSetDir),
      file = file,
      datasetId = None,
      size = size,
      ownerId = Some(ownerId),
      pathInDataset = Some(pathInDataset),
      format = format,
      sha1 = sha1,
      title = title,
      accessibleTo = accessibleTo,
      visibleTo = visibleTo
    )

  /** new folder for a new dataset */
  def apply(sdoSetDir: File,
            ownerId: String,
            pathInDataset: File
           ) =
    // no need to catch exceptions thrown by the constructor as the defaults take care of valid values
    new FileItemSettings(
      sdoSetDir = Some(sdoSetDir),
      datasetId = None,
      ownerId = Some(ownerId),
      pathInDataset = Some(pathInDataset)
    )

  /** new file or folder for an existing dataset */
  def apply(conf: FileItemConf) =
    // no need to catch exceptions thrown by the constructor as FileItemConf performs the same checks
    new FileItemSettings(
      sdoSetDir = conf.sdoSetDir.toOption,
      file = conf.file.toOption,
      datastreamLocation = conf.dsLocation.toOption,
      size = conf.size.toOption,
      accessibleTo = conf.accessibleTo(),
      visibleTo = conf.visibleTo(),
      creatorRole = conf.creatorRole(),
      ownerId = conf.ownerId.toOption.map(_.trim).filter(_.nonEmpty),
      datasetId = conf.datasetId.toOption,
      pathInDataset = conf.pathInDataset.toOption,
      format = conf.format.toOption,
      subordinate = "object" -> s"info:fedora/${conf.datasetId()}"
    ) {
      override def toString = conf.toString
    }
}
