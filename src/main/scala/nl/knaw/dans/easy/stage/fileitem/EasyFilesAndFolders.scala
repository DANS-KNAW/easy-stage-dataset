/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
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
import java.sql.{DriverManager, PreparedStatement}

import nl.knaw.dans.easy.stage.lib.Props.props

import scala.annotation.tailrec
import scala.util.Try

trait EasyFilesAndFolders {
  def getExistingAncestor(file: File, datasetId: String): Try[(String,String)]
}

object EasyFilesAndFolders extends EasyFilesAndFolders{
  lazy val conn = DriverManager.getConnection(props.getString("db-connection-url"))

  def getExistingAncestor(file: File, datasetId: String): Try[(String,String)] = {
    val query: PreparedStatement = conn.prepareStatement(
      s"SELECT pid FROM easy_folders WHERE (path = ? or path = ? || '/') and dataset_sid = '$datasetId'"
    )

    @tailrec
    def get(file: File): (String,String) =
      if(file==null)
        ("",datasetId)
      else {
        query.setString(1, file.getParent)
        query.setString(2, file.getParent)
        val resultSet = query.executeQuery()
        if (resultSet.next())
          (file.getParent, resultSet.getString("pid"))
        else get(file.getParentFile)
      }

    Try {
      try {
        get(file)
      }
      finally {
        query.close()
      }
    }
  }
}
