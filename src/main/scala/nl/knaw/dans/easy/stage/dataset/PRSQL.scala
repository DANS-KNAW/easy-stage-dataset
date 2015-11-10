package nl.knaw.dans.easy.stage.dataset

import java.io.File

import nl.knaw.dans.easy.stage.lib.Constants
import nl.knaw.dans.easy.stage.lib.Constants._
import nl.knaw.dans.easy.stage.lib.Util._

import scala.util.Try

object PRSQL {

  def create(sdoDir:File): Try[Unit] = {
    val prsql =
      <psl:permissionSequenceList xmlns:psl="http://easy.dans.knaw.nl/easy/permission-sequence-list/">
        <sequences></sequences>
      </psl:permissionSequenceList>.toString()
    writeToFile(new File(sdoDir.getPath, PRSQL_FILENAME), prsql)
  }

}
