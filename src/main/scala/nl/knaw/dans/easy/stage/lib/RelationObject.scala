package nl.knaw.dans.easy.stage.lib

import java.io.File

sealed abstract class RelationObject(val relationObjectType: RelationObjectType) {
  def tupled: (String, String)
}

case class SdoRelationObject(folder: File) extends RelationObject(SdoRelationObjectType) {
  override def tupled: (String, String) = (relationObjectType.name, folder.toString)
}

case class FedoraRelationObject(fedoraId: String) extends RelationObject(FedoraRelationObjectType) {
  override def tupled: (String, String) = (relationObjectType.name, s"info:fedora/$fedoraId")
}

sealed abstract class RelationObjectType(val name: String)
private case object FedoraRelationObjectType extends RelationObjectType("object")
private case object SdoRelationObjectType extends RelationObjectType("objectSDO")
