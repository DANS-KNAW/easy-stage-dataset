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
package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.dataset.Licenses
import nl.knaw.dans.easy.stage.lib.Fedora
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime

case class Settings(ownerId: String,
                    submissionTimestamp: DateTime = new DateTime(),
                    bagitDir: File,
                    sdoSetDir: File,
                    URN: Option[String] = None,
                    DOI: Option[String] = None,
                    otherAccessDOI: Boolean = false,
                    fedoraUser: String,
                    fedoraPassword: String,
                    fedoraUrl: URL) {
  Fedora.setFedoraConnectionSettings(fedoraUrl.toString, fedoraUser, fedoraPassword)

  val disciplines: Map[String, String] = Fedora.disciplines
  val licenses: Map[String, File] = Licenses.getLicenses
}

object Settings {

  /** backward compatible call for EasyIngestFlow */
  def apply(ownerId: String,
            submissionTimestamp: String,
            bagitDir: File,
            sdoSetDir: File,
            URN: String,
            DOI: String,
            otherAccessDOI: Boolean,
            fedoraUser: String,
            fedoraPassword: String,
            fedoraUrl: URL) =
    new Settings(ownerId = ownerId,
      submissionTimestamp = DateTime.parse(submissionTimestamp),
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      URN = Some(URN),
      DOI = Some(DOI),
      otherAccessDOI = otherAccessDOI,
      fedoraUser = fedoraUser,
      fedoraPassword = fedoraPassword,
      fedoraUrl = fedoraUrl)

  def apply(conf: Conf, props: PropertiesConfiguration) =
    new Settings(
      ownerId = props.getString("owner"),
      submissionTimestamp = if (conf.submissionTimestamp.isSupplied) conf.submissionTimestamp() else new DateTime(),
      bagitDir = conf.bag(),
      sdoSetDir = conf.sdoSet(),
      URN = conf.urn.get,
      DOI = conf.doi.get,
      otherAccessDOI = conf.otherAccessDOI(),
      fedoraUser = props.getString("fcrepo.user"),
      fedoraPassword = props.getString("fcrepo.password"),
      fedoraUrl = new URL(props.getString("fcrepo.url")))
}
