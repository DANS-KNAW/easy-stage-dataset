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
package nl.knaw.dans.easy.stage.command

import java.io.{ ByteArrayOutputStream, File }

import nl.knaw.dans.easy.stage.CustomMatchers._
import org.apache.commons.configuration.PropertiesConfiguration
import org.rogach.scallop.ScallopConf
import org.scalatest.{ FlatSpec, Matchers }

import scala.collection.JavaConverters._

abstract class AbstractConfSpec extends FlatSpec with Matchers {

  def getCommandLineOptions: ScallopConf

  private val helpInfo = {
    val mockedStdOut = new ByteArrayOutputStream()
    Console.withOut(mockedStdOut) {
      getCommandLineOptions.printHelp()
    }
    mockedStdOut.toString
  }

  "options in help info" should "be part of README.md" in {
    val lineSeparators = s"(${ System.lineSeparator() })+"
    val options = helpInfo.split(s"${ lineSeparators }Options:$lineSeparators")(1)
    options.trim should not be empty
    new File("../README.md") should containTrimmed(options)
  }

  "distributed default properties" should "be valid options" in {
    val optKeys = getCommandLineOptions.builder.opts.map(opt => opt.name).toArray
    val propKeys = new PropertiesConfiguration("src/main/assembly/dist/cfg/application.properties")
      .getKeys.asScala.withFilter(key => key.startsWith("default."))

    propKeys.foreach(key => optKeys should contain(key.replace("default.", "")))
  }
}
