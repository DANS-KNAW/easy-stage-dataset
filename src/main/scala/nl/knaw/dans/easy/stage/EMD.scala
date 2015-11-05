package nl.knaw.dans.easy.stage

import java.io.File
import java.net.URI

import nl.knaw.dans.easy.stage.lib.Constants
import Constants._
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.pf.language.ddm.api.Ddm2EmdCrosswalk
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.binding.EmdMarshaller
import nl.knaw.dans.pf.language.emd.types.{BasicIdentifier, EmdConstants}

import scala.util.{Failure, Success, Try}

object EMD {

  def create(sdoDir: File)(implicit s: Settings): Try[EasyMetadata] = {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists()) {
      return Failure(new RuntimeException(s"Couldn't find metadata/dataset.xml"))
    }
    for {
      emd <- getEasyMetadata(ddm).map(addIdentifiers)
      _   <- writeToFile(new File(sdoDir, EMD_FILENAME), new EmdMarshaller(emd).getXmlString)
    } yield emd
  }

  def getEasyMetadata(ddm: File): Try[EasyMetadata] =
    try {
      val crosswalk = new Ddm2EmdCrosswalk()
      val emd = crosswalk.createFrom(ddm)
      if (emd == null)
        Failure(new RuntimeException(s"${crosswalk.getXmlErrorHandler.getMessages}"))
      else
        Success(emd)
    } catch {
      case t: Throwable => Failure(t)
    }

  private def addIdentifiers(emd: EasyMetadata)(implicit s: Settings): EasyMetadata = {
    val doi = new BasicIdentifier(s.DOI)
    doi.setScheme(if (s.otherAccessDOI) EmdConstants.SCHEME_DOI_OTHER_ACCESS else EmdConstants.SCHEME_DOI)
    doi.setIdentificationSystem(new URI("http://dx.doi.org"))
    emd.getEmdIdentifier.add(doi)
    val pid = new BasicIdentifier(s.URN)
    pid.setScheme(EmdConstants.SCHEME_PID)
    pid.setIdentificationSystem(new URI("http://www.persistent-identifier.nl"))
    emd.getEmdIdentifier.add(pid)
    emd
  }
}
