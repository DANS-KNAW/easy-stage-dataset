package nl.knaw.dans.easy.stage

import java.io.File
import java.sql.{Array, DriverManager}

import scala.util.Try

object EasyFilesAndFolders {

  val conn = DriverManager.getConnection(Props().getString("default.db-connection-url"))

  def getFolder(folder: File, datasetSid: String): Try[String] = Try {
    val statement = conn.prepareStatement("SELECT pid FROM easy_folders (path,dataset_sid) VALUES (?,?)")
    statement.setString(1, folder.toString)
    statement.setString(2, datasetSid)
    statement.closeOnCompletion()
    statement.executeQuery().getString("pid")
  }
}
