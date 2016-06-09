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

case class Settings(
                    submissionTimestamp: DateTime = new DateTime(),
                    depositDir: File,
                    sdoSetDir: File,
                    URN: Option[String] = None,
                    DOI: Option[String] = None,
                    otherAccessDOI: Boolean = false,
                    isMendeley: Boolean,
                    disciplines: Map[String, String]) {

  val licenses: Map[String, File] = Licenses.getLicenses
}

object Settings {

  /** backward compatible call for EasyIngestFlow */
  def apply(
            submissionTimestamp: String,
            depositDir: File,
            sdoSetDir: File,
            URN: String,
            DOI: String,
            otherAccessDOI: Boolean,
            isMendeley: Boolean,
            fedoraUser: String,
            fedoraPassword: String,
            fedoraUrl: URL) = {
    Fedora.setFedoraConnectionSettings(fedoraUrl.toString, fedoraUser, fedoraPassword)
    new Settings(
      submissionTimestamp = DateTime.parse(submissionTimestamp),
      depositDir = depositDir,
      sdoSetDir = sdoSetDir,
      URN = Some(URN),
      DOI = Some(DOI),
      otherAccessDOI = otherAccessDOI,
      isMendeley = isMendeley,
      disciplines = Fedora.disciplines)
  }

  def apply(conf: Conf, props: PropertiesConfiguration) = {
    Fedora.setFedoraConnectionSettings(
      new URL(props.getString("fcrepo.url")).toString,// detour for early validation
      props.getString("fcrepo.user"),
      props.getString("fcrepo.password"))
    new Settings(
      submissionTimestamp = if (conf.submissionTimestamp.isSupplied) conf.submissionTimestamp() else new DateTime(),
      depositDir = conf.deposit(),
      sdoSetDir = conf.sdoSet(),
      URN = conf.urn.get,
      DOI = conf.doi.get,
      otherAccessDOI = conf.otherAccessDOI(),
      isMendeley = conf.isMendeley(),
      disciplines = Fedora.disciplines)
  }
}
