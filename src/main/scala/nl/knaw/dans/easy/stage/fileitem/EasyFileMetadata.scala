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

import scala.util.Try

object EasyFileMetadata {
  def apply(s: FileItemSettings): Try[String] = Try {
      <fimd:file-item-md xmlns:fimd="http://easy.dans.knaw.nl/easy/file-item-md/" version="0.1" >
        <name>{s.pathInDataset.get.getName}</name>
        <path>{s.pathInDataset.get}</path>
        <mimeType>{s.format.getOrElse("application/octet-stream")}</mimeType>
        <size>{s.size.getOrElse(0)}</size>
        <creatorRole>{s.creatorRole}</creatorRole>
        <visibleTo>{s.visibleTo}</visibleTo>
        <accessibleTo>{s.accessibleTo}</accessibleTo>
      </fimd:file-item-md>.toString()
  }
}
