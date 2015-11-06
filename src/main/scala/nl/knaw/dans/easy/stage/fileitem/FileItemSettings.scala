package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.util.UUID

import nl.knaw.dans.easy.stage.lib.Props._
import org.joda.time.DateTime

case class FileItemSettings(sdoSetDir: File,
                            datasetId: String,
                            file: Option[File],
                            ownerId: String = props.getString("owner"),

                            // as in example-bag/metadata/manifest-md5.txt
                            md5: Option[String],
 
                            // as in example-bag/metadata/files.xml
                            filePath: File,
                            identifier: Option[String],// TODO not used?
                            title: Option[List[String]],// TODO not used?
                            description: Option[String],// TODO not used?
                            format: Option[String],
                            created: Option[DateTime],// TODO not used?
 
                            // as in SDO/*/EASY_FILE_METADATA
                            creatorRole: String = "DEPOSITOR",// TODO hardcoded for datasets
                            visibleTo: String = "ANONYMOUS",// TODO hardcoded for datasets
                            accessibleTo: String = "NONE"// TODO hardcoded for datasets
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
