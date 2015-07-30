package nl.knaw.dans.easy.stage

import java.io.File

case class Settings(ownerId: String,
                    bagStorageLocation: String,
                    bagitDir: File,
                    sdoSetDir: File,
                    URN: String,
                    DOI: String,
                    disciplines: Map[String,String])
