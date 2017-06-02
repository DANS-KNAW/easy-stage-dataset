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
package nl.knaw.dans.easy.stage

import java.io.File
import java.net.{ URI, URL }
import java.nio.file.Path

import com.yourmediashelf.fedora.client.FedoraCredentials
import nl.knaw.dans.easy.stage.dataset.Licenses
import nl.knaw.dans.easy.stage.lib.Fedora
import org.apache.commons.configuration.PropertiesConfiguration
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
                    disciplines: Map[String, String]) {

  val licenses: Map[String, File] = Licenses.getLicenses
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
            credentials: FedoraCredentials
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
      disciplines = Fedora.disciplines)
  }

  def apply(conf: Conf, props: PropertiesConfiguration): Settings = {
    Fedora.setFedoraConnectionSettings(
      new URL(props.getString("fcrepo.url")).toString,// detour for early validation
      props.getString("fcrepo.user"),
      props.getString("fcrepo.password"))
    new Settings(
      ownerId = getUserId(conf.deposit()),
      submissionTimestamp = if (conf.submissionTimestamp.isSupplied) conf.submissionTimestamp() else new DateTime(),
      bagitDir = getBagDir(conf.deposit()).get,
      sdoSetDir = conf.sdoSet(),
      urn = conf.urn.toOption,
      doi = conf.doi.toOption,
      otherAccessDoi = conf.otherAccessDOI(),
      fileUris = conf.getDsLocationMappings,
      state = conf.state(),
      disciplines = Fedora.disciplines)
  }

  private def getBagDir(depositDir: File): Option[File] = depositDir.listFiles.find(f => f.isDirectory && f.getName != ".git")
  private def getUserId(depositDir: File) : String = new PropertiesConfiguration(new File(depositDir, "deposit.properties")).getString("depositor.userId")
}
