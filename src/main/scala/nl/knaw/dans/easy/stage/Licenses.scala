package nl.knaw.dans.easy.stage

import java.io.File

import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.LoggerFactory
import scala.collection.JavaConversions._
import scala.collection.mutable

object Licenses {
  val log = LoggerFactory.getLogger(getClass)
  def getLicenses: Map[String, File] = {
    val licDir = new File(System.getProperty("app.home"), "lic")
    val licenses = new PropertiesConfiguration(new File(licDir, "licenses.properties"))
    val map = licenses.getKeys.foldRight(mutable.Map[String, File]())(
      (k: String, m: mutable.Map[String, File]) => {
        m.put(k, new File(licDir, licenses.getString(k)))
        m
      }
    ).toMap
    log.debug(map.toString)
    map
  }
}
