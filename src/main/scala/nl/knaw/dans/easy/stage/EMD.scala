package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.Util._
import nl.knaw.dans.pf.language.ddm.api.Ddm2EmdCrosswalk
import nl.knaw.dans.pf.language.emd.EasyMetadata
import nl.knaw.dans.pf.language.emd.binding.EmdMarshaller

import scala.util.{Success, Failure, Try}

object EMD {

  def create(sdoDir: File)(implicit s: Settings): Try[EasyMetadata] = {
    val ddm = new File(s.bagitDir, "metadata/dataset.xml")
    if (!ddm.exists()) {
      return Failure(new RuntimeException(s"Couldn't find ${sdoDir.getName}/metadata/dataset.xml"))
    }
    for {
      emd <- getEasyMetadata(ddm)
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

}
