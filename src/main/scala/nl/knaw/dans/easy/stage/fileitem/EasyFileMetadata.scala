package nl.knaw.dans.easy.stage.fileitem

import scala.xml.Elem

object EasyFileMetadata {
  def apply(s: FileItemSettings): Elem =
      <fimd:file-item-md xmlns:fimd="http://easy.dans.knaw.nl/easy/file-item-md/" version="0.1" >
        <name>{s.pathInDataset.get.getName}</name>
        <path>{s.pathInDataset.get}</path>
        <mimeType>{s.format.getOrElse("application/octet-stream")}</mimeType>
        <size>{s.pathInStorage.get.length}</size>
        <creatorRole>{s.creatorRole}</creatorRole>
        <visibleTo>{s.visibleTo}</visibleTo>
        <accessibleTo>{s.accessibleTo}</accessibleTo>
      </fimd:file-item-md>
}
