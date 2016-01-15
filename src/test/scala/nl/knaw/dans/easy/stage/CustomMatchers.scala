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

import java.io.{FileInputStream, File}

import org.apache.commons.io.FileUtils._
import org.apache.commons.io.IOUtils
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.words.ResultOfATypeInvocation

import scala.util.{Success, Failure, Try}


/** See also <a href="http://www.scalatest.org/user_guide/using_matchers#usingCustomMatchers">CustomMatchers</a> */
trait CustomMatchers {

  class ContentMatcher(content: String) extends Matcher[File] {
    def apply(left: File) = {
      def trimLines(s: String): String = s.split("\n").map(_.trim).mkString("\n")
      MatchResult(
        trimLines(readFileToString(left)).contains(trimLines(content)),
        s"$left did not contain: $content" ,
        s"$left contains $content"
      )
    }
  }

  class SameContentMatcher(file: File) extends Matcher[File] {
    def apply(left: File) = {
      val leftContent = IOUtils.toString(new FileInputStream(left))
      val fileContent = IOUtils.toString(new FileInputStream(file))
      MatchResult(
        leftContent == fileContent,
        s"$left did not have same content as $file", // TODO add {leftContent diff fileContent} when it works
        s"$left has same content as $file"
      )
    }
  }

  /** usage example: new File(...) should containTrimmed("...") */
  def containTrimmed(content: String) = new ContentMatcher(content)

  /** usage example: new File(...) should haveSameContentAs("...") */
  def haveSameContentAs(file: File) = new SameContentMatcher(file)
}

object CustomMatchers extends CustomMatchers