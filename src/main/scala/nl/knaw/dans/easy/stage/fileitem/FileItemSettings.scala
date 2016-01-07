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

import nl.knaw.dans.easy.stage.lib.Props._

case class FileItemSettings(sdoSetDir: Option[File],
                            datasetId: Option[String] = None,
                            pathInStorage: Option[File] = None,
                            ownerId: String = props.getString("owner"),
                            storageBaseUrl: String = props.getString("storage-base-url"),
                            pathInDataset: Option[File],
                            format: Option[String] = None,

                            // as in SDO/*/EASY_FILE_METADATA
                            creatorRole: String = "DEPOSITOR",
                            visibleTo: String = "ANONYMOUS",
                            accessibleTo: String = "NONE",

                            subordinate: (String, String) = "objectSDO" -> "dataset"
                           )

object FileItemSettings {

  def apply(conf: FileItemConf): FileItemSettings =
    new FileItemSettings(
      sdoSetDir = conf.sdoSetDir.get,
      pathInStorage = conf.pathInStorage.get,
      datasetId = conf.datasetId.get,
      pathInDataset = conf.pathInDataset.get,
      format = conf.format.get,
      subordinate = "object" -> s"info:fedora/${conf.datasetId()}"
    ) {
      override def toString = conf.toString
    }

  def apply(args: Seq[String]): FileItemSettings =
    FileItemSettings(new FileItemConf(args))
}
