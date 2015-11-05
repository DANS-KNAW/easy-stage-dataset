package nl.knaw.dans.easy.stage.lib

import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{FedoraClient, FedoraCredentials}

import scala.collection.JavaConversions._
import scala.xml.XML

object Fedora {

  if (!FedoraRequest.isDefaultClientSet) {
    // the condition prevents overruling a DefaultClient set by easy-ingest-flow
    Fedora.connect(
      Props().getString("fcrepo-service-url"),
      Props().getString("fcrepo-user"),
      Props().getString("fcrepo-password")
    )
  }

  def connect(fedoraServiceUrl: String, fedoraUser: String, fedoraPassword: String): Unit = {
    val credentials = new FedoraCredentials(fedoraServiceUrl, fedoraUser, fedoraPassword)
    FedoraRequest.setDefaultClient(new FedoraClient(credentials))
  }

  lazy val disciplines: Map[String,String] = {
    find("pid~easy-discipline:*")
      .map(pid => (FedoraClient.getRelationships(pid).execute().getEntity(classOf[String]), pid))
      .map{ case (xml, pid) => ((XML.loadString(xml) \\ "Description" \ "setSpec").text, pid) }
      .map{ case (s, pid) => (s.split(':').toSeq.last, pid) }
      .toMap
  }

  def findObjects(query: String): List[String] = find(query).toList

  private def find(query: String) = {
    FedoraClient.findObjects().query(query)
      .maxResults(Integer.MAX_VALUE).pid().execute().getPids
  }
}
