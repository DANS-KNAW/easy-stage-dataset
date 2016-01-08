/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.sql.{DriverManager, PreparedStatement}

import nl.knaw.dans.easy.stage.lib.Props.props
import org.slf4j.LoggerFactory

import scala.util.Try

object EasyFilesAndFolders {
  val log = LoggerFactory.getLogger(getClass)

  val conn = DriverManager.getConnection(props.getString("db-connection-url"))

  def getPathId(path: File, datasetSid: String): Try[Option[String]] = Try {
    val query: PreparedStatement = conn.prepareStatement("SELECT pid FROM easy_folders WHERE dataset_sid = ? and path = ?")
    query.setString(2, path.toString)
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

  def getExistingAncestor(pathInDataset: String, datasetId: String): Try[Option[String]] = Try {
    val query: PreparedStatement = conn.prepareStatement("SELECT count(pid) FROM easy_folders WHERE path = ?")
    try {
      val ancestors = pathInDataset
        .split("/")
        .scanLeft("")((acc, next) => acc + next + "/")
        .reverse
        .map(_.replaceAll("/$", ""))
        .filter(!_.isEmpty)
      ancestors.find(path => {
        query.setString(1, path)
        val resultSet = query.executeQuery()
        if (!resultSet.next()) throw new RuntimeException("Count query returned no rows (?) A count query should ALWAYS return one row")
        resultSet.getString("count") == "1"
      })
    }
    finally {
      query.close()
    }
  }

}
