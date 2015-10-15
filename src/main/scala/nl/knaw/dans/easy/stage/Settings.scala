package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URL

case class Settings(ownerId: String,
                    submissionTimestamp: String,
                    bagStorageLocation: String,
                    bagitDir: File,
                    sdoSetDir: File,
                    URN: String,
                    DOI: String,
                    otherAccessDOI: Boolean,
                    fedoraUser: String,
                    fedoraPassword: String,
                    fedoraUrl: URL) {
  val disciplines: Map[String,String] = Fedora.loadDisciplines(fedoraUrl.toString, fedoraUser, fedoraPassword)
}
