package nl.knaw.dans.easy.stage

import org.scalatest.{Matchers, FlatSpec}

import nl.knaw.dans.easy.stage.dataset.AdditionalLicense._

class AdditionalLicenseSpec extends FlatSpec with Matchers {
  "hasXsiType" should
    """
      |return true if
      |  * type attribute is XMLSchema-instance AND
      |  * attribute value prefix points to expected namespace AND
      |  * attribute value label matches
      |
    """.stripMargin in {
    val xml = <doc
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xmlns:the_namespace="http://the_namespace"
    >
      <element xsi:type="the_namespace:the_label" />
    </doc>
    val elem = (xml \\ "element").head
    hasXsiType(elem, "http://the_namespace", "the_label") shouldBe true
  }

  it should "ignore other attributes" in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
        <element some_attribute="some value" xsi:type="the_namespace:the_label" some_other_attribute="some value"  />
      </doc>
    val elem = (xml \\ "element").head
    hasXsiType(elem, "http://the_namespace", "the_label") shouldBe true
  }

  it should "return false if 'type' attribute is from other namespace than XMLSchema-instance" in {
    val xml =
      <doc xmlns:xsi="NOT http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">"
        <element xsi:type="the_namespace:the_label" />
      </doc>
    val elem = (xml \\ "element").head
    hasXsiType(elem, "http://the_namespace", "the_label") shouldBe false
  }

  it should "return false if attribute value prefix points to wrong namespace" in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
        <element xsi:type="NOT the_namespace:the_label" />
      </doc>
    val elem = (xml \\ "element").head
    hasXsiType(elem, "http://the_namespace", "the_label") shouldBe false
  }

  it should "return false if attribute value label does not match" in {
    val xml =
      <doc xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
           xmlns:the_namespace="http://the_namespace">
        <element xsi:type="the_namespace:not_the_label" />
      </doc>
    val elem = (xml \\ "element").head
    hasXsiType(elem, "http://the_namespace", "the_label") shouldBe false
  }

  it should "return false if no attributes found" in {
    hasXsiType(<elementWithNoAttributes />, "http://the_namespace", "the_label") shouldBe false
  }


  "getLicenseMimeType" should "return 'text/html' if extension is .html" in {
    getLicenseMimeType("/some/license/file.html") shouldBe "text/html"
  }

  it should "return 'text/plain' if extension is .txt" in {
    getLicenseMimeType("/some/license/file.txt") shouldBe "text/plain"
  }

  it should "throw an exception if any other extension is used" in {
    val expectedException = the [IllegalArgumentException] thrownBy getLicenseMimeType("/some/license/file.pdf")
    expectedException.getMessage should include("Unknown extension")
  }


}
