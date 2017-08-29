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
import java.net.URI
import java.nio.file.Path

import com.yourmediashelf.fedora.client.FedoraCredentials
import nl.knaw.dans.easy.stage.lib.Fedora
import org.joda.time.DateTime

case class Settings(ownerId: String,
                    submissionTimestamp: DateTime = new DateTime(),
                    bagitDir: File,
                    sdoSetDir: File,
                    urn: Option[String] = None,
                    doi: Option[String] = None,
                    otherAccessDoi: Boolean = false,
                    fileUris: Map[Path, URI] = Map(),
                    state: String,
                    archive: String,
                    disciplines: Map[String, String],
                    databaseUrl: String,
                    databaseUser: String,
                    databasePassword: String,
                    licenses: Map[String, File])

object Settings {

  /** for EasyIngestFlow */
  def apply(depositorId: String,
            submissionTimestamp: DateTime,
            bagDir: File,
            sdoSetDir: File,
            urn: Option[String],
            doi: Option[String],
            otherAccessDoi: Boolean,
            fileUris: Map[Path, URI],
            state: String,
            archive: String,
            credentials: FedoraCredentials,
            databaseUrl: String,
            databaseUser: String,
            databasePassword: String,
            licenses: Map[String, File]): Settings = {
    Fedora.setFedoraConnectionSettings(
      credentials.getBaseUrl.toString,
      credentials.getUsername,
      credentials.getPassword
    )
    new Settings(
      ownerId = depositorId,
      submissionTimestamp = submissionTimestamp,
      bagitDir = bagDir,
      sdoSetDir = sdoSetDir,
      urn = urn,
      doi = doi,
      otherAccessDoi = otherAccessDoi,
      fileUris = fileUris,
      state = state,
      archive = archive,
      disciplines = Fedora.disciplines,
      databaseUrl = databaseUrl,
      databaseUser = databaseUser,
      databasePassword = databasePassword,
      licenses = licenses)
  }
}
