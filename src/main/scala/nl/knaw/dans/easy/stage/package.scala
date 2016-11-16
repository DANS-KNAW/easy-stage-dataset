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

package object stage {

  // exceptions
  case class RejectedDepositException(msg: String = "", cause: Throwable = null) extends Exception(msg, cause)

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
}
