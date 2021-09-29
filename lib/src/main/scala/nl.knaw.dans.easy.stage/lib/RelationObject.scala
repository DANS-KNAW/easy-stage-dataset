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
package nl.knaw.dans.easy.stage.lib

import java.io.File

import nl.knaw.dans.easy.stage.FedoraID

sealed abstract class RelationObject(val relationObjectType: RelationObjectType) {
  def tupled: (String, String)
}

case class SdoRelationObject(folder: File) extends RelationObject(SdoRelationObjectType) {
  override def tupled: (String, String) = (relationObjectType.name, folder.toString)
}

case class FedoraRelationObject(fedoraId: FedoraID) extends RelationObject(FedoraRelationObjectType) {
  override def tupled: (String, String) = (relationObjectType.name, s"info:fedora/$fedoraId")
}

sealed abstract class RelationObjectType(val name: String)
private case object FedoraRelationObjectType extends RelationObjectType("object")
private case object SdoRelationObjectType extends RelationObjectType("objectSDO")
