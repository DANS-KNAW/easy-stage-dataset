package nl.knaw.dans.easy.stage.lib

import java.io.File

import Constants._
import nl.knaw.dans.easy.stage.lib.Util._

import scala.util.Try

object EasyFileMetadata {
  def create(sdoDir:File, file: File, mimeType: String, relativePath: String): Try[Unit] = {
    val efm =
      <fimd:file-item-md xmlns:fimd="http://easy.dans.knaw.nl/easy/file-item-md/" version="0.1" >
        <name>{file.getName}</name>
        <path>{relativePath}</path>
        <mimeType>{mimeType}</mimeType>
        <size>{file.length}</size>
        <creatorRole>DEPOSITOR</creatorRole>
        <visibleTo>ANONYMOUS</visibleTo>
        <accessibleTo>NONE</accessibleTo>
      </fimd:file-item-md>.toString()
    writeToFile(new File(sdoDir.getPath, EASY_FILE_METADATA_FILENAME), efm)
  }
}
