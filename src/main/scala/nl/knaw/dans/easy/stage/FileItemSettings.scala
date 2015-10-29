package nl.knaw.dans.easy.stage

import java.io.File
import java.util.UUID

import org.joda.time.DateTime

case class FileItemSettings(file: Option[File],
                            datasetId: String,
                            override val sdoSetDir: File,
 
                            // as in example-bag/metadata/manifest-md5.txt
                            md5: String,
 
                            // as in example-bag/metadata/files.xml
                            filePath: File,
                            identifier: Option[UUID],
                            title: Option[List[String]],
                            description: Option[String],
                            format: Option[String],
                            created: Option[DateTime],
 
                            // as in SDO/*/EASY_FILE_METADATA
                            creatorRole: String = "DEPOSITOR",
                            visibleTo: String = "ANONYMOUS",
                            accessibleTo: String = "NONE"
                           ) extends SharedSettings(sdoSetDir, ???)
object FileItemSettings {
  def apply(conf: FileItemConf): FileItemSettings = new FileItemSettings(
    sdoSetDir = conf.sdoSetDir.apply(),
    file = conf.file.get,
    created = conf.created.get,
    description = conf.description.get,
    datasetId = conf.datasetId.apply(),
    filePath = conf.filePath.apply(),
    format = conf.format.get,
    identifier =conf.identifier.get,
    md5 = conf.md5.apply(),
    title = conf.title.get
  )
}
