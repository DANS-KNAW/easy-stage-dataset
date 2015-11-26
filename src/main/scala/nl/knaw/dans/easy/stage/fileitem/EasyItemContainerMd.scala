package nl.knaw.dans.easy.stage.fileitem

import java.io.File

object EasyItemContainerMd {

  def apply(folder: File): String = {
      <icmd:item-container-md xmlns:icmd="http://easy.dans.knaw.nl/easy/item-container-md/" version="0.1">
        <name>{folder.getName}</name>
        <path>{folder.toString}</path>
      </icmd:item-container-md>.toString()
  }
}
