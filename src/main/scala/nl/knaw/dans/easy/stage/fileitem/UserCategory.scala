/**
 * Copyright (C) 2015-2016 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.easy.stage.fileitem

import nl.knaw.dans.common.lang.dataset.AccessCategory
import nl.knaw.dans.common.lang.dataset.AccessCategory._

/** Enumeration of the file properties VisibleTo and AccessibleTo, read "... to a user category" */
object UserCategory extends Enumeration {

  type UserCategory = Value
  val
  ANONYMOUS, // a user that is not logged in
  KNOWN, // a logged in user
  RESTRICTED_REQUEST, // a user that received permission to access the dataset
  RESTRICTED_GROUP, // a user belonging to the same group as the dataset
  NONE // none of the above
  = Value

  private val rightsMap = Map[AccessCategory, UserCategory.Value](
    ANONYMOUS_ACCESS -> ANONYMOUS,
    OPEN_ACCESS_FOR_REGISTERED_USERS -> KNOWN,
    GROUP_ACCESS -> RESTRICTED_GROUP,
    REQUEST_PERMISSION -> RESTRICTED_REQUEST,
    OPEN_ACCESS -> ANONYMOUS,
    // deprecated keys
    NO_ACCESS -> NONE, // used by Excel2EasyMetadataXMLTask.java for invalid values in some spread-sheet cell
    ACCESS_ELSEWHERE -> NONE,
    FREELY_AVAILABLE -> ANONYMOUS // used for thumbnails in EASY-v1
  )

  // map should be exhaustive
  require(AccessCategory.values().toSet == rightsMap.keySet)

  /**
    *
    * @param s toString value of the desired category
    * @return
    */
  def valueOf(s: String): Option[UserCategory.Value] =
    UserCategory.values.find(v => v.toString == s)

  /** gets the default category of users that have download permission for files in a new dataset
    *
    * @param datasetAccesCategory from the EMD of the dataset
    * @return
    */
  def accessibleTo(datasetAccesCategory: AccessCategory): UserCategory.Value =
    rightsMap.get(datasetAccesCategory).get

  /** gets the default category of users that have visibility permission for files in a new dataset
    *
    * @param datasetAccesCategory from the EMD of the dataset
    * @return
    */
  def visibleTo(datasetAccesCategory: AccessCategory): UserCategory.Value =
    rightsMap.get(datasetAccesCategory).get
}
