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
package nl.knaw.dans.easy.stage

import java.io.File
import java.nio.file.Paths

import nl.knaw.dans.easy.stage.lib.Fedora
import nl.knaw.dans.lib.error._
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime

object Command extends App {

  val configuration = Configuration(Paths.get(System.getProperty("app.home")))
  val clo = new CommandLineOptions(args, configuration)
  Fedora.setFedoraConnectionSettings(
    configuration.properties.getString("fcrepo.url"),
    configuration.properties.getString("fcrepo.user"),
    configuration.properties.getString("fcrepo.password"))
  implicit val settings: Settings = new Settings(
    ownerId = getUserId(clo.deposit()),
    submissionTimestamp = if (clo.submissionTimestamp.isSupplied) clo.submissionTimestamp()
                          else new DateTime(),
    bagitDir = getBagDir(clo.deposit()).get,
    sdoSetDir = clo.sdoSet(),
    urn = clo.urn.toOption,
    doi = clo.doi.toOption,
    otherAccessDoi = clo.otherAccessDOI(),
    fileUris = clo.getDsLocationMappings,
    state = clo.state(),
    archive = clo.archive(),
    disciplines = Fedora.disciplines,
    databaseUrl = configuration.properties.getString("db-connection-url"),
    databaseUser = configuration.properties.getString("db-connection-user"),
    databasePassword = configuration.properties.getString("db-connection-password"),
    licenses = configuration.licenses)

  EasyStageDataset.run
    .doIfSuccess(_ => println("OK: Completed succesfully"))
    .doIfFailure { case e => println(s"FAILED: ${ e.getMessage }") }

  private def getBagDir(depositDir: File): Option[File] = {
    depositDir.listFiles.find(f => f.isDirectory && f.getName != ".git")
  }

  private def getUserId(depositDir: File): String = {
    new PropertiesConfiguration(new File(depositDir, "deposit.properties"))
      .getString("depositor.userId")
  }
}
