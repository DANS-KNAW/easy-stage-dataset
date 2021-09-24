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

import com.yourmediashelf.fedora.client.FedoraCredentials
import nl.knaw.dans.easy.stage.lib.Fedora
import org.joda.time.DateTime

import java.io.File
import java.net.URI
import java.nio.file.Path

/**
 *
 * @param ownerId
 * @param submissionTimestamp
 * @param bagitDir
 * @param sdoSetDir empty directory
 * @param urn
 * @param doi
 * @param otherAccessDoi
 * @param fileUris  ignored when skipPayload is true
 * @param state
 * @param archive
 * @param disciplines
 * @param databaseUrl
 * @param databaseUser
 * @param databasePassword
 * @param licenses
 * @param includeBagMetadata
 * @param skipPayload when no doi is provided payloads are skipped anyway
 * @param extraDescription
 */
case class Settings(ownerId: String,
                    submissionTimestamp: DateTime = DateTime.now,
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
                    licenses: Map[String, File],
                    includeBagMetadata: Boolean,
                    skipPayload: Boolean,
                    extraDescription: Option[String] = None,
                   ) {
  override def toString: String = {
    s"Stage-Dataset.Settings(ownerId = $ownerId, submissionTimestamp = $submissionTimestamp, " +
      s"bagitDir = $bagitDir, sdoSetDir = $sdoSetDir, urn = ${ urn.getOrElse("<not defined>") }, " +
      s"doi = ${ doi.getOrElse("<not defined>") }, otherAccessDoi = $otherAccessDoi, " +
      s"fileUris = $fileUris, state = $state, archive = $archive, " +
      s"Database($databaseUrl, $databaseUser, ****), licenses = $licenses, includeBagMetadata = $includeBagMetadata, " +
      s"skipPayload = $skipPayload, extraDescription = $extraDescription)"
  }
}

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
            licenses: Map[String, File],
            includeBagMetadata: Boolean,
            skipPayload: Boolean,
            extraDescription: Option[String],
           ): Settings = {
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
      licenses = licenses,
      includeBagMetadata = includeBagMetadata,
      skipPayload = skipPayload,
      extraDescription = extraDescription,
    )
  }
}
