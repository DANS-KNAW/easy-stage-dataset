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

object FileAccessCategory extends Enumeration {
  type FileAccessCategory = Value
  val ANONYMOUS, KNOWN, RESTRICTED_REQUEST, RESTRICTED_GROUP, NONE = Value

  val accessibleTo = Map[AccessCategory,FileAccessCategory.Value](
    ANONYMOUS_ACCESS -> ANONYMOUS,
    OPEN_ACCESS -> ANONYMOUS,
    OPEN_ACCESS_FOR_REGISTERED_USERS -> KNOWN,
    GROUP_ACCESS -> RESTRICTED_GROUP,
    REQUEST_PERMISSION -> RESTRICTED_REQUEST,
    NO_ACCESS -> NONE,
    // not defined in https://github.com/DANS-KNAW/easy-app/blob/master/lib/easy-business/src/main/java/nl/knaw/dans/easy/domain/model/VisibleTo.java
    ACCESS_ELSEWHERE -> NONE,
    FREELY_AVAILABLE -> ANONYMOUS)

  val visibleTo = Map[AccessCategory,FileAccessCategory.Value](
    ANONYMOUS_ACCESS -> ANONYMOUS,
    OPEN_ACCESS -> ANONYMOUS,
    OPEN_ACCESS_FOR_REGISTERED_USERS -> KNOWN,
    GROUP_ACCESS -> RESTRICTED_GROUP,
    REQUEST_PERMISSION -> RESTRICTED_REQUEST,
    NO_ACCESS -> NONE,
    ACCESS_ELSEWHERE -> NONE,
    // not defined in https://github.com/DANS-KNAW/easy-app/blob/master/lib/easy-business/src/main/java/nl/knaw/dans/easy/domain/model/AccessibleTo.java
    FREELY_AVAILABLE -> ANONYMOUS)
}
