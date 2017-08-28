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
package nl.knaw.dans.easy.stage.dataset

import java.io.File

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.configuration.PropertiesConfiguration

import scala.collection.JavaConversions._
import scala.collection.mutable

object Licenses extends DebugEnhancedLogging {
  def getLicenses: Map[String, File] = {
    val licDir = new File(System.getProperty("app.home"), "lic")
    val licenses = new PropertiesConfiguration(new File(licDir, "licenses.properties"))
    val map = licenses.getKeys.foldRight(mutable.Map[String, File]())(
      (k: String, m: mutable.Map[String, File]) => {
        m.put(k, new File(licDir, licenses.getString(k)))
        m
      }
    ).toMap
    logger.debug(map.toString)
    map
  }
}
