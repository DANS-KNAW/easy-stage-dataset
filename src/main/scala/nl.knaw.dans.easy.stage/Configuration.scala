package nl.knaw.dans.easy.stage

import java.io.File
import java.nio.file.{ Files, Paths }

import org.apache.commons.configuration.PropertiesConfiguration
import resource.managed

import scala.collection.JavaConverters._
import scala.io.Source

case class Configuration(version: String, properties: PropertiesConfiguration, licenses: Map[String, File])

object Configuration {

  def apply(): Configuration = {
    val home = Paths.get(System.getProperty("app.home"))
    val cfgPath = Seq(Paths.get(s"/etc/opt/dans.knaw.nl/easy-stage-dataset/"), home.resolve("cfg"))
      .find(Files.exists(_))
      .getOrElse { throw new IllegalStateException("No configuration directory found") }

    new Configuration(
      version = managed(Source.fromFile(home.resolve("bin/version").toFile)).acquireAndGet(_.mkString),
      properties = new PropertiesConfiguration() {
        setDelimiterParsingDisabled(true)
        load(cfgPath.resolve("application.properties").toFile)
      },
      licenses = {
        val licDir = home.resolve("lic")
        val licenses = new PropertiesConfiguration(licDir.resolve("licenses.properties").toFile)
        licenses.getKeys.asScala.map(key => key -> licDir.resolve(licenses.getString(key)).toFile).toMap
      }
    )
  }
}
