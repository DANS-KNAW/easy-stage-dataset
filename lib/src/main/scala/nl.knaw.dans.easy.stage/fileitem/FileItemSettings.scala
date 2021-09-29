/*
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
import java.net.URL

import nl.knaw.dans.easy.stage.fileitem.FileAccessRights.UserCategory
import nl.knaw.dans.easy.stage.fileitem.FileItemSettings._
import nl.knaw.dans.easy.stage.lib.{ Constants, Fedora, FedoraRelationObject, RelationObject, SdoRelationObject }
import resource.{ ManagedResource, _ }

case class FileItemSettings(sdoSetDir: Option[File],
                            file: Option[File] = None,
                            datasetId: Option[String],
                            datastreamLocation: Option[URL] = None,
                            size: Option[Long] = None,
                            ownerId: Option[String] = None,
                            pathInDataset: Option[File],
                            title: Option[String] = None,
                            format: Option[String] = None,
                            sha1: Option[String] = None,

                            // as in SDO/*/EASY_FILE_METADATA
                            creatorRole: String = defaultCreatorRole,
                            visibleTo: UserCategory = FileAccessRights.NONE,
                            accessibleTo: UserCategory = FileAccessRights.ANONYMOUS,
                            fedora: Fedora = Fedora,
                            easyFilesAndFolders: EasyFilesAndFolders,

                            subordinate: RelationObject = SdoRelationObject(new File(Constants.DATASET_SDO))) {
  require(FileItemSettings.creatorRoles.contains(creatorRole), s"illegal value for creatorRole, got $creatorRole")
}

object FileItemSettings {
  val defaultCreatorRole = "DEPOSITOR"
  val creatorRoles = Array("ARCHIVIST", "DEPOSITOR")

  /** new file for a new dataset */
  def apply(sdoSetDir: File,
            file: Option[File],
            datastreamLocation: Option[URL],
            ownerId: String,
            pathInDataset: File,
            format: Option[String],
            sha1: Option[String],
            title: Option[String],
            size: Option[Long],
            visibleTo: UserCategory,
            accessibleTo: UserCategory,
            databaseUrl: String,
            databaseUser: String,
            databasePassword: String): ManagedResource[FileItemSettings] = {
    managed(new EasyFilesAndFoldersImpl(databaseUrl, databaseUser, databasePassword))
      .map(filesAndFolders => new FileItemSettings(
        sdoSetDir = Some(sdoSetDir),
        file = file,
        datastreamLocation = datastreamLocation,
        datasetId = None,
        size = size,
        ownerId = Some(ownerId),
        pathInDataset = Some(pathInDataset),
        format = format,
        sha1 = sha1,
        title = title,
        accessibleTo = accessibleTo,
        visibleTo = visibleTo,
        easyFilesAndFolders = filesAndFolders
      ))
  }

  /** new folder for a new dataset */
  def apply(sdoSetDir: File,
            ownerId: String,
            pathInDataset: File,
            databaseUrl: String,
            databaseUser: String,
            databasePassword: String): ManagedResource[FileItemSettings] = {
    managed(new EasyFilesAndFoldersImpl(databaseUrl, databaseUser, databasePassword))
      .map(filesAndFolders => new FileItemSettings(
        sdoSetDir = Some(sdoSetDir),
        datasetId = None,
        ownerId = Some(ownerId),
        pathInDataset = Some(pathInDataset),
        easyFilesAndFolders = filesAndFolders
      ))
  }
}
