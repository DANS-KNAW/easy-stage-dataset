/*
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

import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{ FedoraClient, FedoraCredentials }

import scala.annotation.tailrec
import scala.collection.JavaConverters._
import scala.xml.XML

trait Fedora {
  def findObjects(query: String, acc: Seq[String] = Nil, token: Option[String] = None): Seq[String]
}

object Fedora extends Fedora {
  val findObjectsBatchSize = 25

  def setFedoraConnectionSettings(fedoraServiceUrl: String, fedoraUser: String, fedoraPassword: String): Unit = {
    val credentials = new FedoraCredentials(fedoraServiceUrl, fedoraUser, fedoraPassword)
    FedoraRequest.setDefaultClient(new FedoraClient(credentials))
  }

  lazy val disciplines: Map[String, String] =
    findObjects("pid~easy-discipline:*")
      .map(pid => (FedoraClient.getRelationships(pid).execute().getEntity(classOf[String]), pid))
      .map { case (xml, pid) => ((XML.loadString(xml) \\ "Description" \ "setSpec").text, pid) }
      .map { case (s, pid) => (s.split(':').toSeq.last, pid) }
      .toMap

  @tailrec
  override def findObjects(query: String, acc: Seq[String] = Nil, token: Option[String] = None): Seq[String] = {
    val objectsQuery = FedoraClient.findObjects().maxResults(findObjectsBatchSize).pid().query(query)
    val objectsResponse = token.map(objectsQuery.sessionToken(_).execute).getOrElse(objectsQuery.execute)

    if (objectsResponse.hasNext)
      findObjects(query, acc ++ objectsResponse.getPids.asScala, Some(objectsResponse.getToken))
    else
      acc ++ objectsResponse.getPids.asScala
  }
}
