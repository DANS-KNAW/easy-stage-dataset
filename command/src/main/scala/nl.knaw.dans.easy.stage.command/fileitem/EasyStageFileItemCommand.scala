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
package nl.knaw.dans.easy.stage.command.fileitem

import java.nio.file.Paths
import java.sql.SQLException

import com.yourmediashelf.fedora.client.FedoraClientException
import nl.knaw.dans.easy.stage.command.Configuration
import nl.knaw.dans.easy.stage.fileitem.{ EasyFilesAndFoldersImpl, EasyStageFileItem, FileItemSettings }
import nl.knaw.dans.easy.stage.lib._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource._

import scala.util.{ Success, Try }

object EasyStageFileItemCommand extends DebugEnhancedLogging {

  def main(args: Array[String]) {
    val configuration = Configuration(Paths.get(System.getProperty("app.home")))
    val clo = new FileItemCommandLineOptions(args, configuration)
    Fedora.setFedoraConnectionSettings(
      configuration.properties.getString("fcrepo.url"),
      configuration.properties.getString("fcrepo.user"),
      configuration.properties.getString("fcrepo.password"))
    getSettingsRows(clo, configuration)
      .map(seqOfSettings =>
        for (settings <- seqOfSettings;
             s <- settings) {
          EasyStageFileItem.run(s)
            .map(_ => logger.info(s"Staging SUCCESS of $settings"))
            .recover { case t: Throwable =>
              logger.error(s"Staging FAIL of $settings", t)
              if (t.isInstanceOf[SQLException] || t.isInstanceOf[FedoraClientException]) return
            }
        })
      .recover { case t: Throwable => logger.error(s"Staging FAIL of $clo with repo url ${ configuration.properties.getString("fcrepo.url") }", t) }
  }

  def getSettingsRows(clo: FileItemCommandLineOptions, configuration: Configuration): Try[Seq[ManagedResource[FileItemSettings]]] = {
    if (clo.datasetId.isDefined)
      Success(
        managed(new EasyFilesAndFoldersImpl(
          databaseUrl = configuration.properties.getString("db-connection-url"),
          databaseUser = configuration.properties.getString("db-connection-user"),
          databasePassword = configuration.properties.getString("db-connection-password")))
          .map(FileItemApplySettings(clo, _)) :: Nil)
    else {
      val trailArgs = Seq(clo.sdoSetDir().toString)
      CSV(clo.csvFile(), clo.longOptionNames)
        .map {
          case (csv, warning) =>
            for (w <- warning)
              logger.warn(w)

            val rows = csv.getRows
            if (rows.isEmpty) logger.warn("Empty CSV file")
            rows.map(options => {
              logger.info(s"Options: ${ options.mkString(" ") }")
              managed(new EasyFilesAndFoldersImpl(
                databaseUrl = configuration.properties.getString("db-connection-url"),
                databaseUser = configuration.properties.getString("db-connection-user"),
                databasePassword = configuration.properties.getString("db-connection-password")))
                .map(FileItemApplySettings(new FileItemCommandLineOptions(options ++ trailArgs, configuration), _))
            })
        }
    }
  }
}
