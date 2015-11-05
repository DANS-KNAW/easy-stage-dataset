package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.lib.Constants
import Constants._
import nl.knaw.dans.easy.stage.lib.Util._
import org.joda.time.DateTime

import scala.util.Try
import scala.xml.Elem

object AMD {

  def create(sdoDir: File)(implicit s: Settings): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, AMD_FILENAME), AMD(s.ownerId, s.submissionTimestamp).toString())

  /*
   * An exact timestamp is required, so valid ISO dates like 2015-09-01T12:01 won't do
   */
  def normalizeTimestamp (t: String): String = DateTime.parse(t).toString

  def apply(depositorId: String, submissionTimestamp: String): Elem = {
    val normalizedSubmissiontimestamp = normalizeTimestamp(submissionTimestamp)
    <damd:administrative-md xmlns:damd="http://easy.dans.knaw.nl/easy/dataset-administrative-metadata/" version="0.1">
      <datasetState>SUBMITTED</datasetState>
      <previousState>DRAFT</previousState>
      <lastStateChange>{normalizedSubmissiontimestamp}</lastStateChange>
      <depositorId>{depositorId}</depositorId>
      <stateChangeDates>
        <damd:stateChangeDate>
          <fromState>DRAFT</fromState>
          <toState>SUBMITTED</toState>
          <changeDate>{normalizedSubmissiontimestamp}</changeDate>
        </damd:stateChangeDate>
      </stateChangeDates>
      <groupIds></groupIds>
      <damd:workflowData version="0.1">
        <assigneeId>NOT_ASSIGNED</assigneeId>
        <wfs:workflow xmlns:wfs="http://easy.dans.knaw.nl/easy/workflow/">
          <id>dataset</id>
          <remarks></remarks>
          <steps>
            <wfs:workflow>
              <id>dataset.sip</id>
              <timeSpentWritable>true</timeSpentWritable>
              <remarks></remarks>
              <steps>
                <wfs:workflow>
                  <id>dataset.sip.files</id>
                  <remarks></remarks>
                  <steps>
                    <wfs:workflow>
                      <id>dataset.sip.files.completeness</id>
                      <required>true</required>
                      <remarks></remarks>
                      <steps></steps>
                    </wfs:workflow>
                    <wfs:workflow>
                      <id>dataset.sip.files.accessibility</id>
                      <required>true</required>
                      <remarks></remarks>
                      <steps></steps>
                    </wfs:workflow>
                    <wfs:workflow>
                      <id>dataset.sip.files.privacy</id>
                      <required>true</required>
                      <remarks></remarks>
                      <steps></steps>
                    </wfs:workflow>
                  </steps>
                </wfs:workflow>
                <wfs:workflow>
                  <id>dataset.sip.file-list</id>
                  <remarks></remarks>
                  <steps>
                    <wfs:workflow>
                      <id>dataset.sip.file-list.file-metadata</id>
                      <remarks></remarks>
                      <steps></steps>
                    </wfs:workflow>
                  </steps>
                </wfs:workflow>
                <wfs:workflow>
                  <id>dataset.sip.descriptive-metadata</id>
                  <remarks></remarks>
                  <steps>
                    <wfs:workflow>
                      <id>dataset.sip.descriptive-metadata.completeness</id>
                      <required>true</required>
                      <remarks></remarks>
                      <steps></steps>
                    </wfs:workflow>
                    <wfs:workflow>
                      <id>dataset.sip.descriptive-metadata.identifiers</id>
                      <remarks></remarks>
                      <steps></steps>
                    </wfs:workflow>
                  </steps>
                </wfs:workflow>
              </steps>
            </wfs:workflow>
            <wfs:workflow>
              <id>dataset.aip</id>
              <timeSpentWritable>true</timeSpentWritable>
              <remarks></remarks>
              <steps>
                <wfs:workflow>
                  <id>dataset.aip.file-conversion</id>
                  <remarks></remarks>
                  <steps></steps>
                </wfs:workflow>
                <wfs:workflow>
                  <id>dataset.aip.file-metadata</id>
                  <remarks></remarks>
                  <steps></steps>
                </wfs:workflow>
                <wfs:workflow>
                  <id>dataset.aip.structure</id>
                  <remarks></remarks>
                  <steps></steps>
                </wfs:workflow>
              </steps>
            </wfs:workflow>
            <wfs:workflow>
              <id>dataset.dip</id>
              <timeSpentWritable>true</timeSpentWritable>
              <remarks></remarks>
              <steps>
                <wfs:workflow>
                  <id>dataset.dip.publish-files</id>
                  <required>true</required>
                  <remarks></remarks>
                  <steps></steps>
                </wfs:workflow>
                <wfs:workflow>
                  <id>dataset.dip.jumpoff</id>
                  <remarks></remarks>
                  <steps></steps>
                </wfs:workflow>
                <wfs:workflow>
                  <id>dataset.dip.relations</id>
                  <remarks></remarks>
                  <steps></steps>
                </wfs:workflow>
              </steps>
            </wfs:workflow>
          </steps>
        </wfs:workflow>
      </damd:workflowData>
    </damd:administrative-md>
  }
}
