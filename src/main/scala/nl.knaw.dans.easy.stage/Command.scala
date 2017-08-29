package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.lib.Fedora
import nl.knaw.dans.lib.error._
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime

object Command extends App {

  val configuration = Configuration()
  val clo = new CommandLineOptions(args, configuration)
  Fedora.setFedoraConnectionSettings(
    configuration.properties.getString("fcrepo.url"),
    configuration.properties.getString("fcrepo.user"),
    configuration.properties.getString("fcrepo.password"))
  implicit val settings: Settings = new Settings(
    ownerId = getUserId(clo.deposit()),
    submissionTimestamp = if (clo.submissionTimestamp.isSupplied) clo.submissionTimestamp()
                          else new DateTime(),
    bagitDir = getBagDir(clo.deposit()).get,
    sdoSetDir = clo.sdoSet(),
    urn = clo.urn.toOption,
    doi = clo.doi.toOption,
    otherAccessDoi = clo.otherAccessDOI(),
    fileUris = clo.getDsLocationMappings,
    state = clo.state(),
    archive = clo.archive(),
    disciplines = Fedora.disciplines,
    databaseUrl = configuration.properties.getString("db-connection-url"),
    databaseUser = configuration.properties.getString("db-connection-user"),
    databasePassword = configuration.properties.getString("db-connection-password"),
    licenses = configuration.licenses)

  EasyStageDataset.run
    .doIfSuccess(_ => println("OK: Completed succesfully"))
    .doIfFailure { case e => println(s"FAILED: ${ e.getMessage }") }

  private def getBagDir(depositDir: File): Option[File] = {
    depositDir.listFiles.find(f => f.isDirectory && f.getName != ".git")
  }

  private def getUserId(depositDir: File): String = {
    new PropertiesConfiguration(new File(depositDir, "deposit.properties"))
      .getString("depositor.userId")
  }
}
