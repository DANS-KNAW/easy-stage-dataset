package nl.knaw.dans.easy.stage

import java.net.{ HttpURLConnection, URL }

import scala.util.Try

trait CanConnectFixture {

  def canConnect(urls: Array[String]): Boolean = Try {
    urls.map(url => {
      new URL(url).openConnection match {
        case connection: HttpURLConnection =>
          connection.setConnectTimeout(1000)
          connection.setReadTimeout(1000)
          connection.connect()
          connection.disconnect()
          true
        case _ => throw new Exception
      }
    })
  }.isSuccess
}
