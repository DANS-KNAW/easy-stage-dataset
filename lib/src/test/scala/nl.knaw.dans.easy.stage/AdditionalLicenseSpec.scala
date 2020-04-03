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
package nl.knaw.dans.easy.stage

import nl.knaw.dans.easy.stage.dataset.AdditionalLicense._
import org.scalatest.Inside
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class AdditionalLicenseSpec extends AnyFlatSpec with Matchers with Inside {

  "hasXsiType" should
    """
      |return true if
      |  * type attribute is XMLSchema-instance AND
      |  * attribute value prefix points to expected namespace AND
      |  * attribute value label matches
      |
    """.stripMargin in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
      <element xsi:type="the_namespace:the_label" />
    </doc>

    val elems = xml \\ "element"
    elems match {
      case es @ Seq() => es should not be empty
      case Seq(elem, _ @ _*) => hasXsiType(elem, "http://the_namespace", "the_label") shouldBe true
    }
  }

  it should "ignore other attributes" in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
        <element some_attribute="some value" xsi:type="the_namespace:the_label" some_other_attribute="some value"  />
      </doc>
    val elems = xml \\ "element"
    elems match {
      case es @ Seq() => es should not be empty
      case Seq(elem, _ @ _*) => hasXsiType(elem, "http://the_namespace", "the_label") shouldBe true
    }
  }

  it should "return false if 'type' attribute is from other namespace than XMLSchema-instance" in {
    val xml =
      <doc xmlns:xsi="NOT http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">"
        <element xsi:type="the_namespace:the_label" />
      </doc>
    val elems = xml \\ "element"
    elems match {
      case es @ Seq() => es should not be empty
      case Seq(elem, _ @ _*) => hasXsiType(elem, "http://the_namespace", "the_label") shouldBe false
    }
  }

  it should "return false if attribute value prefix points to wrong namespace" in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
        <element xsi:type="NOT the_namespace:the_label" />
      </doc>
    val elems = xml \\ "element"
    elems match {
      case es @ Seq() => es should not be empty
      case Seq(elem, _ @ _*) => hasXsiType(elem, "http://the_namespace", "the_label") shouldBe false
    }
  }

  it should "return false if attribute value label does not match" in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
        <element xsi:type="the_namespace:not_the_label" />
      </doc>
    val elems = xml \\ "element"
    elems match {
      case es @ Seq() => es should not be empty
      case Seq(elem, _ @ _*) => hasXsiType(elem, "http://the_namespace", "the_label") shouldBe false
    }
  }

  it should "return false if no attributes found" in {
    hasXsiType(<elementWithNoAttributes />, "http://the_namespace", "the_label") shouldBe false
  }
}
