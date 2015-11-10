package nl.knaw.dans.easy.stage.fileitem

import java.io.File

import nl.knaw.dans.easy.stage.lib.Props._
import org.joda.time.DateTime

case class FileItemSettings(sdoSetDir: File,
                            // only used for EasyStageFileItem.run,
                            // not for createFileSdo as called by EasyStageDataset
                            datasetId: String = "",
                            file: Option[File] = None,
                            ownerId: String = props.getString("owner"),
                            storageBaseUrl: String = props.getString("storage-base-url"),

                            // as in example-bag/metadata/manifest-md5.txt
                            md5: Option[String] = None,
 
                            // as in example-bag/metadata/files.xml
                            filePath: File,
                            identifier: Option[String] = None,// TODO not used?
                            title: Option[List[String]] = None ,// TODO not used?
                            description: Option[String] = None,// TODO not used?
                            format: Option[String] = None,
                            created: Option[DateTime] = None,// TODO not used?
 
                            // as in SDO/*/EASY_FILE_METADATA
                            creatorRole: String = "DEPOSITOR",
                            visibleTo: String = "ANONYMOUS",
                            accessibleTo: String = "NONE"
                           )

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
    md5 = conf.md5.get,
    title = conf.title.get
  ) {
    override def toString = conf.builder.args.mkString(", ")
  }
  def apply(args: Seq[String]): FileItemSettings =
    FileItemSettings( new FileItemConf(args))
}
