package nl.knaw.dans.easy.stage.lib

import java.io.File

import nl.knaw.dans.easy.stage.fileitem.EasyStageFileItem._
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory

object Props {
  def apply(): PropertiesConfiguration = props
  val file = new File(System.getProperty("app.home"), "cfg/application.properties")
  val props = new PropertiesConfiguration(file)
}
