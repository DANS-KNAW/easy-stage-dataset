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

object EasyItemContainerMd {

  def apply(path: String): String = {
      <icmd:item-container-md xmlns:icmd="http://easy.dans.knaw.nl/easy/item-container-md/" version="0.1">
        <name>{path.split("/").last}</name>
        <path>{path}</path>
      </icmd:item-container-md>.toString()
  }
}
