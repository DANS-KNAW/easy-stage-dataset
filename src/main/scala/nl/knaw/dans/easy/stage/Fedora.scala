package nl.knaw.dans.easy.stage

import java.io.File

import com.yourmediashelf.fedora.client.request.FedoraRequest
import com.yourmediashelf.fedora.client.{FedoraClient, FedoraCredentials}
import org.apache.commons.configuration.PropertiesConfiguration

import scala.collection.JavaConversions._
import scala.xml.XML

object Fedora {

  def connect(fedoraServiceUrl: String, fedoraUser: String, fedoraPassword: String): Unit = {
    val credentials = new FedoraCredentials(fedoraServiceUrl, fedoraUser, fedoraPassword)
    FedoraRequest.setDefaultClient(new FedoraClient(credentials))
  }

  def connect(): Unit = {
    val file = new File(System.getProperty("app.home"), "cfg/application.properties")
    val props = new PropertiesConfiguration(file)
    Fedora.connect(
      props.getString("fcrepo-service-url"),
      props.getString("fcrepo-user"),
      props.getString("fcrepo-password")
    )
  }

  lazy val disciplines: Map[String,String] = {
    FedoraClient.findObjects().query("pid~easy-discipline:*")
      .maxResults(Integer.MAX_VALUE).pid().execute().getPids
      .map(pid => (FedoraClient.getRelationships(pid).execute().getEntity(classOf[String]), pid))
      .map{ case (xml, pid) => ((XML.loadString(xml) \\ "Description" \ "setSpec").text, pid) }
      .map{ case (s, pid) => (s.split(':').toSeq.last, pid) }
      .toMap
  }
}
