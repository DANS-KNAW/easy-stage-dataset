package nl.knaw.dans.easy.stage.fileitem

import nl.knaw.dans.easy.stage.AbstractConfSpec
import org.rogach.scallop.ScallopConf

class FileItemConfSpec extends AbstractConfSpec {

  override def getConf: ScallopConf = new FileItemConf("-".split(" "))
}