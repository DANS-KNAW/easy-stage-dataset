package nl.knaw.dans.easy.stage.fileitem

import java.io.File

import nl.knaw.dans.easy.stage.lib.Props._
import org.joda.time.DateTime

case class FileItemSettings(sdoSetDir: Option[File],
                            datasetId: Option[String] = None,
                            file: Option[File] = None,
                            ownerId: String = props.getString("owner"),
                            storageBaseUrl: String = props.getString("storage-base-url"),

                            // as in example-bag/metadata/manifest-md5.txt
                            md5: Option[String] = None,
 
                            // as in example-bag/metadata/files.xml
                            filePath: Option[File],
                            identifier: Option[String] = None,// TODO not used?
                            title: Option[List[String]] = None ,// TODO not used?
                            description: Option[String] = None,// TODO not used?
                            format: Option[String] = None,
                            created: Option[DateTime] = None,// TODO not used?
 
                            // as in SDO/*/EASY_FILE_METADATA
                            creatorRole: String = "DEPOSITOR",
                            visibleTo: String = "ANONYMOUS",
                            accessibleTo: String = "NONE",

                            subordinate: (String, String) = "objectSDO" -> "dataset"
                           )

object FileItemSettings {

  def apply(conf: FileItemConf): FileItemSettings =
    new FileItemSettings(
      sdoSetDir = conf.sdoSetDir.get,
      file = conf.file.get,
      created = conf.created.get,
      description = conf.description.get,
      datasetId = conf.datasetId.get,
      filePath = conf.filePath.get,
      format = conf.format.get,
      identifier =conf.identifier.get,
      md5 = conf.md5.get,
      title = conf.title.get,
      subordinate = "object" -> conf.datasetId()
    ) {
      override def toString = conf.toString
    }

  def apply(args: Seq[String]): FileItemSettings =
    FileItemSettings(new FileItemConf(args))
}
