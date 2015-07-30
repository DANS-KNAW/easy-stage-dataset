package nl.knaw.dans.easy.stage

import com.yourmediashelf.fedora.client.{FedoraClient, FedoraCredentials}

import scala.collection.JavaConversions._
import scala.xml.XML

object Fedora {

  def loadDisciplines(): Map[String,String] = {
    val credentials = new FedoraCredentials("http://deasy:8080/fedora", "fedoraAdmin", "fedoraAdmin") // TODO: parametrise this
    val client = new FedoraClient(credentials)
    FedoraClient.findObjects().query("pid~easy-discipline:*").maxResults(Integer.MAX_VALUE).pid().execute(client).getPids
      .map(pid => (FedoraClient.getRelationships(pid).execute(client).getEntity(classOf[String]), pid))
      .map{ case (xml, pid) => ((XML.loadString(xml) \\ "Description" \ "setSpec").text, pid) }
      .map{ case (disciplines, pid) => (disciplines.split(':').toSeq.last, pid) }
      .toMap
  }

}
