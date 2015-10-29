package nl.knaw.dans.easy.stage

import java.io.File

class SharedSettings(sdoSetDirectory: File, bagitDirectory: File) {
  val sdoSetDir: File = sdoSetDirectory
  val bagitDir: File = bagitDirectory
}
