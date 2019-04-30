package nl.knaw.dans.easy.stage.dataset

import java.io.File
import java.nio.file.{ Files, Paths }

import nl.knaw.dans.easy.stage.Settings
import org.apache.commons.io.FileUtils
import org.scalatest.FlatSpec

class AgreementDataSpec extends FlatSpec {
  private val testDir = Paths.get("target/test", getClass.getSimpleName)
  FileUtils.deleteQuietly(testDir.toFile)
  Files.createDirectories(testDir)

  def newSettings(bagitDir: File): Settings = {
    new Settings(
      ownerId = "",
      bagitDir = bagitDir,
      sdoSetDir = testDir.toFile,
      state = "DRAFT",
      archive = "EASY",
      disciplines = Map.empty,
      databaseUrl = "",
      databaseUser = "",
      databasePassword = "",
      licenses = Map.empty)
  }

  "it" should "get the job done" in {


    println(testDir)
  }


}
