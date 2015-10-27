package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.EasyStageDataset._
import nl.knaw.dans.easy.stage.Util._
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory

import scala.util.{Try, Failure, Success}

object EasyStageFileItem {
  val log = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]) {
    val props = new PropertiesConfiguration(new File(System.getProperty("app.home"), "cfg/application.properties"))
    val conf = new FileItemConf(args)
    println("hello world")
    implicit val s = new FilItemSettings()
    run match {
      case Success(_) => log.info("Staging SUCCESS")
      case Failure(t) => log.error("Staging FAIL", t)
    }
  }

  def run(implicit s: FilItemSettings): Try[Unit] = {
    log.debug(s"settings = $s")
    ???
  }
}
