package nl.knaw.dans.easy.stage.lib

import java.io.File

import org.apache.commons.configuration.PropertiesConfiguration

object Props {
  def apply(): PropertiesConfiguration = props
  val file = new File(System.getProperty("app.home"), "cfg/application.properties")
  val props = new PropertiesConfiguration(file)
}
