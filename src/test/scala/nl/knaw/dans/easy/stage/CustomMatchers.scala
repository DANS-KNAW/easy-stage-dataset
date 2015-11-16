package nl.knaw.dans.easy.stage

import java.io.File

import org.apache.commons.io.FileUtils._
import org.scalatest.matchers.{MatchResult, Matcher}
import org.scalatest.words.ResultOfATypeInvocation

import scala.util.{Success, Failure, Try}


/** See also <a href="http://www.scalatest.org/user_guide/using_matchers#usingCustomMatchers">CustomMatchers</a> */
trait CustomMatchers {

  class ExceptionMatcher(expectedMessage: String) extends Matcher[Try[Any]] {

    def apply(left: Try[Any]) = {

      MatchResult(
        left match {
          case Failure(t) => t.getMessage == expectedMessage
          case _ => false
        },
        rawFailureMessage = s"did not fail with message [$expectedMessage]",
        rawNegatedFailureMessage = s"failed with message [$expectedMessage]"
      )
    }
  }

  /** usage example: Try[Any] should failWithMessage ("some message") */
  def failWithMessage(messageFragment: String) = new ExceptionMatcher(messageFragment: String)

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

  /** usage example: new File(...) should containTrimmed("...") */
  def containTrimmed(content: String) = new ContentMatcher(content)
}

object CustomMatchers extends CustomMatchers