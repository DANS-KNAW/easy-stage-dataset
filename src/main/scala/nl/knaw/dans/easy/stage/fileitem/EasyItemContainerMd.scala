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
