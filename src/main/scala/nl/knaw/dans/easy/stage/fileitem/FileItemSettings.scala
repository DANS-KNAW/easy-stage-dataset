package nl.knaw.dans.easy.stage.fileitem

import java.io.File

import nl.knaw.dans.easy.stage.lib.Props._

case class FileItemSettings(sdoSetDir: Option[File],
                            datasetId: Option[String] = None,
                            pathInStorage: Option[File] = None,
                            ownerId: String = props.getString("owner"),
                            storageBaseUrl: String = props.getString("storage-base-url"),
                            pathInDataset: Option[File],
                            format: Option[String] = None,

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
      pathInStorage = conf.pathInStorage.get,
      datasetId = conf.datasetId.get,
      pathInDataset = conf.pathInDataset.get,
      format = conf.format.get,
      subordinate = "object" -> s"info:fedora/${conf.datasetId()}"
    ) {
      override def toString = conf.toString
    }

  def apply(args: Seq[String]): FileItemSettings =
    FileItemSettings(new FileItemConf(args))
}
