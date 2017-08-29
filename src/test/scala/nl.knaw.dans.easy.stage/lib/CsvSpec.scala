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
package nl.knaw.dans.easy.stage.lib

import java.io.ByteArrayInputStream
import java.nio.file.Paths

import nl.knaw.dans.easy.stage.Configuration
import nl.knaw.dans.easy.stage.fileitem.FileItemCommandLineOptions
import org.apache.commons.configuration.PropertiesConfiguration
import org.scalatest.{ FlatSpec, Inside, Matchers }

import scala.util.Failure

class CsvSpec extends FlatSpec with Matchers with Inside {
  private val resourceDirString: String = Paths.get(getClass.getResource("/").toURI).toAbsolutePath.toString

  private val mockedConfiguration = new Configuration("version x.y.z", new PropertiesConfiguration() {
    setDelimiterParsingDisabled(true)
    load(Paths.get(resourceDirString + "/debug-config", "application.properties").toFile)
  }, Map.empty)

  // TODO copied from FileItemConfSpec. make fixture for it
  private def clo = new FileItemCommandLineOptions("-i i -d http:// -p p -s 0 --format f outdir".split(" "), mockedConfiguration)

  "apply" should "fail with too few headers in the input" in {
    val in = new ByteArrayInputStream("FORMAT,DATASET-ID,xxx,STAGED-DIGITAL-OBJECT-SET".getBytes)

    inside(CSV(in, clo.longOptionNames)) {
      case Failure(e) => e should have message
        "Missing columns: PATH-IN-DATASET, DATASTREAM-LOCATION, SIZE, FILE-LOCATION, ACCESSIBLE-TO, VISIBLE-TO, CREATOR-ROLE, OWNER-ID"
    }
  }

  it should "fail with uppercase in any of the required headers" in {
    val in = new ByteArrayInputStream("".stripMargin.getBytes)

    inside(CSV(in, Seq("ABc", "def"))) {
      case Failure(e) => e.getMessage should include("not supported")
    }
  }
}
