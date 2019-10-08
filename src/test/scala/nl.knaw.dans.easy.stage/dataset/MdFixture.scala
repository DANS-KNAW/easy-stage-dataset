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
import java.nio.file.Files

import nl.knaw.dans.easy.stage.{ CanConnectFixture, Settings }
import org.apache.commons.io.FileUtils
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{ DateTime, DateTimeUtils }
import org.scalatest.{ BeforeAndAfterEach, FlatSpec, Inside, Matchers }

class MdFixture extends FlatSpec with Matchers with Inside with CanConnectFixture with BeforeAndAfterEach {

  val testDir = new File(s"target/test/${ getClass.getSimpleName }")
  val sdoSetDir = new File(s"$testDir/sdoSet")

  val nowYMD = "2018-03-22"
  val now = s"${ nowYMD }T21:43:01.576"
  val nowUTC = s"${ nowYMD }T20:43:01Z"
  /** Causes DateTime.now() to return a predefined value. */
  DateTimeUtils.setCurrentMillisFixed(new DateTime(nowUTC).getMillis)
  val nowIso = DateTime.now.toString(ISODateTimeFormat.dateTime())

  def newSettings(bagitDir: File): Settings = {
    new Settings(
      ownerId = "",
      bagitDir = bagitDir,
      sdoSetDir = sdoSetDir,
      state = "DRAFT",
      archive = "EASY",
      disciplines = Map.empty,
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "",
      licenses = Map.empty,
      stageDatasetVersion = "test",
    )
  }

  override def beforeEach(): Unit = {
    if (Files.exists(testDir.toPath)) {
      FileUtils.deleteDirectory(testDir)
    }
  }
}
