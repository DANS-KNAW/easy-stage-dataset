package nl.knaw.dans.easy.stage

import java.io.File

import org.apache.commons.io.FileUtils

import scala.util.Try
import scala.xml.XML

object AdditionalLicense {

  def create(sdo: File)(implicit s: Settings): Try[Option[File]] =
    for {
      original <- getAdditionalLicense()
      copied <- copyAdditionalLicense(original, sdo)
    } yield copied

  def getAdditionalLicense()(implicit s: Settings): Try[Option[File]] = Try {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists) {
      throw new RuntimeException("Unable to find `metadata/dataset.xml` in bag.")
    }
    val licenses = XML.loadFile(ddm) \\ "DDM" \ "dcmiMetadata" \ "license"
    licenses match {
      case Seq() => None
      case Seq(license) => Some(s.licenses(license.text))
      case licences => sys.error(s"Found ${licences.size} dcterms:license elements. Only one additional license allowed")
    }
  }

  def copyAdditionalLicense(file: Option[File], sdo: File): Try[Option[File]] = Try {
    file.map(f => {
      val additionalLicenseFile = new File(sdo, Constants.ADDITIONAL_LICENSE)
      FileUtils.copyFile(f, additionalLicenseFile)
      additionalLicenseFile
    })
  }
}
