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
package nl.knaw.dans.easy.stage.command.fileitem

import nl.knaw.dans.easy.stage.fileitem.{EasyFilesAndFolders, FileItemSettings}
import nl.knaw.dans.easy.stage.lib.FedoraRelationObject

object FileItemApplySettings {
  /** new file or folder for an existing dataset */
  def apply(conf: FileItemCommandLineOptions, easyFilesAndFolders: EasyFilesAndFolders) =
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
      subordinate = FedoraRelationObject(conf.datasetId()),
      easyFilesAndFolders = easyFilesAndFolders) {
      override def toString: String = conf.toString
    }
}
