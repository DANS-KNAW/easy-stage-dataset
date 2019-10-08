/**
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
package nl.knaw.dans.easy.stage.dataset

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat

import scala.xml.Elem

object AMD extends DebugEnhancedLogging {
  type AdministrativeMetadata = Elem

  def apply(depositorId: String, submissionTimestamp: DateTime, state: String, remarks: DepositorInfo, stageDatasetVersion: String): AdministrativeMetadata = {
    val remarksContent =
      s"""${ remarks.privacySensitiveRemark }
         |
         |${ remarks.messageFromDepositor }
         |""".stripMargin.trim
    trace(depositorId, submissionTimestamp, state)
    <damd:administrative-md xmlns:damd="https://easy.dans.knaw.nl/easy/dataset-administrative-metadata/" version="0.1">
      <datasetState>{state}</datasetState>{
        if (state != "DRAFT") {
          <previousState>DRAFT</previousState>
          <lastStateChange>{submissionTimestamp}</lastStateChange>
        }
      }
      <depositorId>{depositorId}</depositorId>
      {
        if (state == "DRAFT") {
          <stateChangeDates />
        }
        else {
          <stateChangeDates>
            <damd:stateChangeDate>
              <fromState>DRAFT</fromState>
              <toState>{ state }</toState>
              <changeDate>{ submissionTimestamp }</changeDate>
            </damd:stateChangeDate>
          </stateChangeDates>
        }
      }
      <groupIds />
      <damd:workflowData version="0.1">
        <assigneeId>NOT_ASSIGNED</assigneeId>
        <wfs:workflow xmlns:wfs="https://easy.dans.knaw.nl/easy/workflow/">
          <id>dataset</id>
          <remarks>
          {
            if (remarksContent.nonEmpty)
              <remark>
                <text>{ remarksContent }</text>
                <remarkerId>{ s"easy-stage-dataset_$stageDatasetVersion" }</remarkerId>
                <remarkDate>{ DateTime.now().toString(ISODateTimeFormat.dateTime()) }</remarkDate>
              </remark>
          }
          </remarks>
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
