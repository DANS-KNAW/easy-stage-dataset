package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.sql.{PreparedStatement, DriverManager}

import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import nl.knaw.dans.easy.stage.lib.Props
import nl.knaw.dans.easy.stage.lib.Props.props
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

object EasyFilesAndFolders {
  val log = LoggerFactory.getLogger(getClass)

  val conn = DriverManager.getConnection(props.getString("db_connection_url"))

  def getPathId(path: File, datasetSid: String): Try[Option[String]] = Try {
    val query: PreparedStatement = conn.prepareStatement("SELECT pid FROM easy_folders WHERE dataset_sid = ? and path = ?")
    query.setString(2, path.toString +"/")
    query.setString(1, datasetSid)
    log.debug(s"$query")
    query.closeOnCompletion()
    val resultSet = query.executeQuery()
    if (!resultSet.next())
      None
    else {
      val result = resultSet.getString("pid")
      log.debug(s"pathId = $result")
      Some(result)
    }
  }
}
