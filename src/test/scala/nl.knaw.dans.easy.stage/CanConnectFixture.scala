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
package nl.knaw.dans.easy.stage

import java.net.UnknownHostException

import javax.xml.XMLConstants
import javax.xml.transform.Source
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.{ Schema, SchemaFactory }

import scala.util.{ Failure, Try }
import scala.xml.SAXParseException

trait CanConnectFixture {

  def canConnect(urls: Array[String]): Boolean = urls
    .map(url => isAvailable(loadSchema(url)))
    .distinct sameElements Array(true)

  def loadSchema(schema: String): Try[Schema] = {
    Try {
      SchemaFactory
        .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(Array(new StreamSource(schema)).toArray[Source])
    }
  }

  def isAvailable(schema: Try[Any]): Boolean = {
    schema match {
      case Failure(e: SAXParseException) if e.getCause.isInstanceOf[UnknownHostException] => false
      case Failure(e: SAXParseException) if e.getMessage.contains("Cannot resolve") =>
        println("Probably an offline third party schema: " + e.getMessage)
        false
      case _ => true
    }
  }
}
