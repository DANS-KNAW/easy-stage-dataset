/*
 * Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.fileitem

import java.io.File
import java.nio.file.Paths
import java.sql.SQLException

import com.yourmediashelf.fedora.client.FedoraClientException
import nl.knaw.dans.easy.stage._
import nl.knaw.dans.easy.stage.lib.FOXML.{ getDirFOXML, getFileFOXML }
import nl.knaw.dans.easy.stage.lib.Util._
import nl.knaw.dans.easy.stage.lib._
import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import resource._

import scala.annotation.tailrec
import scala.util.{ Failure, Success, Try }

object EasyStageFileItem extends DebugEnhancedLogging {

  def run(implicit s: FileItemSettings): Try[Unit] = {
    trace(s)
    for {
      datasetId <- getValidDatasetId(s)
      sdoSetDir <- mkdirSafe(s.sdoSetDir)
      datasetSdoSetDir <- mkdirSafe(new File(sdoSetDir, datasetId.replace(":", "_")))
      pathInDataset <- Try { s.pathInDataset.get }
      existingAncestor <- s.easyFilesAndFolders.getExistingAncestor(pathInDataset, datasetId)
      _ = createFolderSdos(pathInDataset, datasetSdoSetDir)
      _ <- createFileSdoForExistingDataset(datasetSdoSetDir, existingAncestor)
    } yield ()
  }

  private def createFolderSdos(file: File,
                               datasetSdoSetDir: File
                              )(implicit s: FileItemSettings): Unit = {

    def getPath(file: File): String = {
      if (file.getParentFile == null)
        file.getName
      else
        getPath(file.getParentFile) + "/" + file.getName
    }

    @tailrec
    def createParent(child: File): Unit = {
      if (child != null) {
        val parent = child.getParentFile
        if (parent != null) {
          var grandParent = parent.getParentFile
          if (grandParent == null)
            grandParent = new File(datasetSdoSetDir.getName)
          val sdoDir = new File(datasetSdoSetDir, toSdoName(parent.toString))
          createFolderSdo(sdoDir, getPath(parent), SdoRelationObject(grandParent))
          createParent(parent)
        }
      }
    }

    createParent(file)
  }

  private def createFileSdoForExistingDataset(datasetSdoSetDir: File,
                                              existingAncestor: ExistingAncestor
                                             )(implicit s: FileItemSettings): Try[Unit] = {
    val parentPath = Option(s.pathInDataset.get.getParent).getOrElse("")
    val ancestor = existingAncestor match {
      case ((`parentPath`, fedoraId)) => FedoraRelationObject(fedoraId)
      case _ => SdoRelationObject(new File(toSdoName(parentPath)))
    }
    createFileSdo(new File(datasetSdoSetDir, toSdoName(s.pathInDataset.get.toString)), ancestor)
  }

  def createFileSdo(sdoDir: File, ancestor: RelationObject)(implicit s: FileItemSettings): Try[Unit] = {
    trace(sdoDir, ancestor)
    require(s.datastreamLocation.isDefined != s.file.isDefined, s"Exactly one of datastreamLocation and file must be defined (datastreamLocation = ${ s.datastreamLocation }, file = ${ s.file })")
    debug(s"Creating file SDO: ${ s.pathInDataset.getOrElse("<no path in dataset?>") }")
    sdoDir.mkdir()
    for {
      mime <- Try { s.format.get }
      cfgContent <- Try { JSON.createFileCfg(mime, ancestor, s.subordinate) }
      _ <- writeJsonCfg(sdoDir, cfgContent)
      title <- Try { s.title.getOrElse(s.pathInDataset.get.getName) }
      foxmlContent = getFileFOXML(title, s.ownerId.get, mime)
      _ <- writeFoxml(sdoDir, foxmlContent)
      fmd <- EasyFileMetadata(s)
      _ <- writeFileMetadata(sdoDir, fmd)
      _ <- s.file.flatMap(_ => s.file.map(f =>
        copyFile(sdoDir, f)
      )).getOrElse(Success(Unit))
    } yield ()
  }

  def createFolderSdo(sdoDir: File, path: String, parent: RelationObject)(implicit s: FileItemSettings): Try[Unit] = {
    trace(sdoDir, path, parent)
    sdoDir.mkdir()
    for {
      _ <- writeJsonCfg(sdoDir, JSON.createDirCfg(parent, s.subordinate))
      _ <- writeFoxml(sdoDir, getDirFOXML(path, s.ownerId.get))
      _ <- writeItemContainerMetadata(sdoDir, EasyItemContainerMd(path))
    } yield ()
  }

  private def getValidDatasetId(s: FileItemSettings): Try[String] = {
    s.datasetId
      .map(id => {
        s.fedora.findObjects(s"pid~${ s.datasetId.get }") match {
          case Seq() => Failure(new Exception(s"${ s.datasetId.get } does not exist in repository"))
          case _ => Success(id)
        }
      })
      .getOrElse(Failure(new Exception(s"no datasetId provided")))
  }

  def toSdoName(path: String): String = {
    trace(path)
    path.replaceAll("[/.]", "_").replaceAll("^_", "")
  }
}
