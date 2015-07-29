package nl.knaw.dans.easy.stage

import java.io.File

import nl.knaw.dans.easy.stage.Constants._
import nl.knaw.dans.easy.stage.Util._

import scala.util.Try
import scala.xml.Elem

object AMD {

  def create(sdoDir: File)(implicit s: Settings): Try[Unit] =
    writeToFile(new File(sdoDir.getPath, AMD_FILENAME), AMD(s.ownerId, "2015-07-09T10:38:24.570+02:00").toString())

  def apply(depositorId: String, submissionDate: String): Elem = {
    <damd:administrative-md xmlns:damd="http://easy.dans.knaw.nl/easy/dataset-administrative-metadata/" version="0.1">
      <datasetState>SUBMITTED</datasetState>
      <previousState>DRAFT</previousState>
      <lastStateChange>{submissionDate}</lastStateChange>
      <depositorId>{depositorId}</depositorId>
      <stateChangeDates>
        <damd:stateChangeDate>
          <fromState>DRAFT</fromState>
          <toState>SUBMITTED</toState>
          <changeDate>{submissionDate}</changeDate>
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
