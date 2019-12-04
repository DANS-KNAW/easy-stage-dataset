/**
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.sql.{ Connection, DriverManager }
import java.util.concurrent.atomic.AtomicBoolean

import nl.knaw.dans.easy.stage.ExistingAncestor
import resource._

import scala.util.{ Success, Try }

trait EasyFilesAndFolders {
  def getExistingAncestor(file: File, datasetId: String): Try[ExistingAncestor]
}

class EasyFilesAndFoldersImpl(databaseUrl: String,
                              databaseUser: String,
                              databasePassword: String) extends EasyFilesAndFolders with AutoCloseable {
  val connCreated = new AtomicBoolean(false)
  lazy val conn: Connection = {
    val conn = DriverManager.getConnection(databaseUrl, databaseUser, databasePassword)
    connCreated.compareAndSet(false, true)
    conn
  }

  override def close(): Unit = if (connCreated.compareAndSet(true, false)) conn.close()

  def getExistingAncestor(file: File, datasetId: String): Try[ExistingAncestor] = {
    Option(file)
      .map(file => {
        val query = s"SELECT pid FROM easy_folders WHERE (path = ? or path = ? || '/') and dataset_sid = ?"
        val resultSet = for {
          prepStatement <- managed(conn.prepareStatement(query))
          _ = prepStatement.setString(1, file.getParent)
          _ = prepStatement.setString(2, file.getParent)
          _ = prepStatement.setString(3, datasetId)
          resultSet <- managed(prepStatement.executeQuery())
        } yield resultSet

        resultSet.map(rs => {
          if (rs.next())
            Try { (file.getParent, rs.getString("pid")) }
          else
            getExistingAncestor(file.getParentFile, datasetId)
        }).tried.flatten
      })
      .getOrElse(Success("", datasetId))
  }
}
