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

class UserCategorySpec extends FlatSpec with Matchers {
  /*
  according to ddm.xsd there are 5 options for the dataset AccessRights
        <xs:enumeration value="OPEN_ACCESS">
        <xs:enumeration value="OPEN_ACCESS_FOR_REGISTERED_USERS">
        <xs:enumeration value="GROUP_ACCESS">
        <xs:enumeration value="REQUEST_PERMISSION">
        <xs:enumeration value="NO_ACCESS">
    */

  "visibleTo OPEN_ACCESS" should "properly map" in {
    UserCategory.visibleTo(AccessCategory.OPEN_ACCESS) shouldBe UserCategory.ANONYMOUS
  }

  "visibleTo OPEN_ACCESS_FOR_REGISTERED_USERS)" should "properly map" in {
    UserCategory.visibleTo(AccessCategory.OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe UserCategory.KNOWN
  }
  "visibleTo GROUP_ACCESS" should "properly map" in {
    UserCategory.visibleTo(AccessCategory.GROUP_ACCESS) shouldBe UserCategory.RESTRICTED_GROUP
  }
  "visibleTo REQUEST_PERMISSION)" should "properly map" in {
    UserCategory.visibleTo(AccessCategory.REQUEST_PERMISSION) shouldBe UserCategory.RESTRICTED_REQUEST
  }
  "visibleTo NO_ACCESS)" should "properly map" in {
    UserCategory.visibleTo(AccessCategory.NO_ACCESS) shouldBe UserCategory.NONE
  }

  "accessibleTo OPEN_ACCESS" should "properly map" in {
    UserCategory.accessibleTo(AccessCategory.OPEN_ACCESS) shouldBe UserCategory.ANONYMOUS
  }

  "accessibleTo OPEN_ACCESS_FOR_REGISTERED_USERS)" should "properly map" in {
    UserCategory.accessibleTo(AccessCategory.OPEN_ACCESS_FOR_REGISTERED_USERS) shouldBe UserCategory.KNOWN
  }
  "accessibleTo GROUP_ACCESS" should "properly map" in {
    UserCategory.accessibleTo(AccessCategory.GROUP_ACCESS) shouldBe UserCategory.RESTRICTED_GROUP
  }
  "accessibleTo REQUEST_PERMISSION)" should "properly map" in {
    UserCategory.accessibleTo(AccessCategory.REQUEST_PERMISSION) shouldBe UserCategory.RESTRICTED_REQUEST
  }
  "accessibleTo NO_ACCESS)" should "properly map" in {
    UserCategory.accessibleTo(AccessCategory.NO_ACCESS) shouldBe UserCategory.NONE
  }

  "valueOf" should "succeed with a proper-cased string" in {
    val s = "RESTRICTED_GROUP"
    UserCategory.valueOf(s).get.toString shouldBe s
  }

  it should "fail with a wrong cased string" in {
    UserCategory.valueOf("restricted_group") shouldBe None
  }
}
