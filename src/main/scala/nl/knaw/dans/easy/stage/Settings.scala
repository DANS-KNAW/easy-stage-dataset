package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URL

import nl.knaw.dans.easy.stage.dataset.Licenses
import nl.knaw.dans.easy.stage.lib.Fedora

case class Settings(ownerId: String,
                    submissionTimestamp: String,
                    bagStorageLocation: String,
                    bagitDir: File,
                    sdoSetDir: File,
                    URN: String,
                    DOI: String,
                    otherAccessDOI: Boolean = false,
                    fedoraUser: String,
                    fedoraPassword: String,
                    fedoraUrl: URL) {
  Fedora.connect(fedoraUrl.toString, fedoraUser, fedoraPassword)

  val disciplines: Map[String,String] = Fedora.disciplines
  val licenses: Map[String, File] = Licenses.getLicenses
}
