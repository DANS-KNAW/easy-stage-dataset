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
package nl.knaw.dans.easy

import java.net.{HttpURLConnection, URL}

import nl.knaw.dans.pf.language.ddm.handlermaps.NameSpace

import scala.util.{Failure, Success, Try}
import scala.language.implicitConversions

package object stage {

  case class RejectedDepositException(msg: String, cause: Throwable = null) extends Exception(msg, cause)

  val xsds: Array[String] = Array(NameSpace.DC.uri, NameSpace.DDM.uri)

  def canConnect(urls: Array[String]): Boolean = Try {
    urls.map { url =>
      new URL(url).openConnection match {
        case connection: HttpURLConnection =>
          connection.setConnectTimeout(1000)
          connection.setReadTimeout(1000)
          connection.connect()
          connection.disconnect()
          true
        case _ => throw new Exception
      }
    }
  }.isSuccess


  /*
   Logic opperators that work with Try[Boolean]
   TODO future candidate for dans-scala-lib.
   */
  implicit class TryLogic(val t: Try[Boolean]) extends AnyVal {
    def &&(t2: => Try[Boolean]): Try[Boolean] = {
      t.flatMap {
        case true => t2
        case _ => Success(false)
      }
    }

    // Using DummyImplicit to prevent 'have same type after type erasure' !
    def &&(b: => Boolean)(implicit d: DummyImplicit): Try[Boolean] = {
      t.map {
        case true => b
        case _ => false
      }
    }

    def ||(t2: => Try[Boolean]): Try[Boolean] = {
      t.flatMap {
        case false => t2
        case _ => Success(true)
      }
    }

    // Using DummyImplicit to prevent 'have same type after type erasure' !
    def ||(b: => Boolean)(implicit d: DummyImplicit): Try[Boolean] = {
      t.map {
        case false => b
        case _ => true
      }
    }

    def unary_! : Try[Boolean] = {
      t.map(!_)
    }
  }

  implicit class BooleanLogicWithTry(val b: Boolean) extends AnyVal {
    def &&(t2: => Try[Boolean]): Try[Boolean] = {
      if (b) t2
      else Success(false)
    }

    def ||(t2: => Try[Boolean]): Try[Boolean] = {
      if (b) Success(true)
      else t2
    }
  }

}
