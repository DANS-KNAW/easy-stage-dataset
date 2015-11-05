package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.Util._

import scala.util.Try

object EasyItemContainerMd {

  def create(sdoDir:File, folder: File, relativePath: String): Try[Unit] = {
    val eicmd =
      <icmd:item-container-md xmlns:icmd="http://easy.dans.knaw.nl/easy/item-container-md/" version="0.1">
        <name>{folder.getName}</name>
        <path>{relativePath}</path>
      </icmd:item-container-md>.toString()
    writeToFile(new File(sdoDir.getPath, EASY_ITEM_CONTAINER_MD_FILENAME), eicmd)
  }
}
