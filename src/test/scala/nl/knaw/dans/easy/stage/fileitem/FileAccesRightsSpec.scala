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
import org.scalatest.{FlatSpec, Matchers}

class FileAccesRightsSpec extends FlatSpec with Matchers {
  /*
  according to ddm.xsd there are 5 options for the dataset AccessRights
        <xs:enumeration value="OPEN_ACCESS">
        <xs:enumeration value="OPEN_ACCESS_FOR_REGISTERED_USERS">
        <xs:enumeration value="GROUP_ACCESS">
        <xs:enumeration value="REQUEST_PERMISSION">
        <xs:enumeration value="NO_ACCESS">
    */

  "visibleTo OPEN_ACCESS" should "properly map" in {
    FileAccessRights.visibleTo(AccessCategory.OPEN_ACCESS) shouldBe FileAccessRights.ANONYMOUS
  }

  "visibleTo OPEN_ACCESS_FOR_REGISTERED_USERS)" should "properly map" in {
    FileAccessRights.visibleTo(AccessCategory.OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe FileAccessRights.ANONYMOUS
  }
  "visibleTo GROUP_ACCESS" should "properly map" in {
    FileAccessRights.visibleTo(AccessCategory.GROUP_ACCESS) shouldBe FileAccessRights.ANONYMOUS
  }
  "visibleTo REQUEST_PERMISSION)" should "properly map" in {
    FileAccessRights.visibleTo(AccessCategory.REQUEST_PERMISSION) shouldBe FileAccessRights.ANONYMOUS
  }
  "visibleTo NO_ACCESS)" should "properly map" in {
    FileAccessRights.visibleTo(AccessCategory.NO_ACCESS) shouldBe FileAccessRights.ANONYMOUS
  }

  "accessibleTo OPEN_ACCESS" should "properly map" in {
    FileAccessRights.accessibleTo(AccessCategory.OPEN_ACCESS) shouldBe FileAccessRights.ANONYMOUS
  }

  "accessibleTo OPEN_ACCESS_FOR_REGISTERED_USERS)" should "properly map" in {
    FileAccessRights.accessibleTo(AccessCategory.OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe FileAccessRights.KNOWN
  }
  "accessibleTo GROUP_ACCESS" should "properly map" in {
    FileAccessRights.accessibleTo(AccessCategory.GROUP_ACCESS) shouldBe FileAccessRights.RESTRICTED_GROUP
  }
  "accessibleTo REQUEST_PERMISSION)" should "properly map" in {
    FileAccessRights.accessibleTo(AccessCategory.REQUEST_PERMISSION) shouldBe FileAccessRights.RESTRICTED_REQUEST
  }
  "accessibleTo NO_ACCESS)" should "properly map" in {
    FileAccessRights.accessibleTo(AccessCategory.NO_ACCESS) shouldBe FileAccessRights.NONE
  }

  "valueOf" should "succeed with a proper-cased string" in {
    val s = "RESTRICTED_GROUP"
    FileAccessRights.valueOf(s).get.toString shouldBe s
  }

  it should "fail with a wrong cased string" in {
    FileAccessRights.valueOf("restricted_group") shouldBe None
  }
}
