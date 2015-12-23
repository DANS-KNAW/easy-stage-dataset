package nl.knaw.dans.easy.stage.lib

import com.yourmediashelf.fedora.client.FedoraClient._
import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{FedoraClient, FedoraCredentials}

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.xml.XML

object Fedora {
  val findObjectsBatchSize = 25

  def setFedoraConnectionSettings(fedoraServiceUrl: String, fedoraUser: String, fedoraPassword: String): Unit = {
    val credentials = new FedoraCredentials(fedoraServiceUrl, fedoraUser, fedoraPassword)
    FedoraRequest.setDefaultClient(new FedoraClient(credentials))
  }

  lazy val disciplines: Map[String,String] =
    findObjects("pid~easy-discipline:*")
      .map(pid => (FedoraClient.getRelationships(pid).execute().getEntity(classOf[String]), pid))
      .map { case (xml, pid) => ((XML.loadString(xml) \\ "Description" \ "setSpec").text, pid) }
      .map { case (s, pid) => (s.split(':').toSeq.last, pid) }
      .toMap

  @tailrec
  def findObjects(query: String, acc: Seq[String] = Nil, token: Option[String] = None): Seq[String] = {
    val objectsQuery = FedoraClient.findObjects().maxResults(findObjectsBatchSize).pid().query(query)
    val objectsResponse = token match {
      case None =>
        objectsQuery.execute
      case Some(t) =>
        objectsQuery.sessionToken(t).execute
    }
    if (objectsResponse.hasNext) findObjects(query, acc ++ objectsResponse.getPids,  Some(objectsResponse.getToken))
    else acc ++ objectsResponse.getPids
  }
}
