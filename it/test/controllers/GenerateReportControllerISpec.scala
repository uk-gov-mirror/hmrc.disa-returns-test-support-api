/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.http.Fault
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSResponse
import play.api.test.Helpers.await
import utils.BaseIntegrationSpec

class GenerateReportControllerISpec extends BaseIntegrationSpec {

  val zRef         = "Z1234"
  val invalidZRef  = "1234"
  val year         = "2025-26"
  val invalidYear  = "202526"
  val month        = "FEB"
  val invalidMonth = "XYZ"

  val validJsonBody: String =
    """
      |{
      |  "oversubscribed": 10,
      |  "traceAndMatch": 5,
      |  "failedEligibility": 3
      |}
    """.stripMargin

  val invalidJsonBody: String =
    """
      |{
      |  "oversubscribeddddd": 10,
      |  "traceAndMatch": 5,
      |  "failedEligibility": 3
      |}
    """.stripMargin

  val validParsedJson:   JsValue = Json.parse(validJsonBody)
  val invalidParsedJson: JsValue = Json.parse(invalidJsonBody)

  "POST /monthly/:zRef/:year/:month/reconciliation" should {

    "return 204 NoContent when generate report and callback both succeed" in {
      stubAuth()
      stubGenerateReport(noContent, zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status shouldBe NO_CONTENT
    }

    "return 204 NoContent when lowercase ZRef supplied" in {
      stubAuth()
      stubGenerateReport(noContent, zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRequest(zRef = zRef.toLowerCase, year = year, month = month, body = validParsedJson)

      result.status shouldBe NO_CONTENT
    }

    "return 500 InternalServerError when callback fails" in {
      stubAuth()
      stubGenerateReport(noContent, zRef, year, month)
      stubCallback(serverError, zRef, year, month)

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status shouldBe INTERNAL_SERVER_ERROR
    }

    "return 500 InternalServerError when generateReport fails" in {
      stubAuth()
      stubGenerateReport(serverError, zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe INTERNAL_SERVER_ERROR
      (result.json \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (result.json \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 500 InternalServerError when generateReport throws an exception" in {
      stubAuth()
      stubGenerateReport(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER), zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe INTERNAL_SERVER_ERROR
      (result.json \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (result.json \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 400 BadRequest for invalid oversubscribed field" in {
      stubAuth()
      val result = generateRequest(zRef = zRef, year = year, month = month, body = invalidParsedJson)

      result.status shouldBe BAD_REQUEST

      (result.json \ "code").as[String]    shouldBe "VALIDATION_FAILURE"
      (result.json \ "message").as[String] shouldBe "Bad request"

      val errors = (result.json \ "errors").as[Seq[JsValue]]
      (errors.head \ "code").as[String]    shouldBe "MISSING_FIELD"
      (errors.head \ "message").as[String] shouldBe "This field is required"
      (errors.head \ "path").as[String]    shouldBe "/oversubscribed"

    }
    "return 400 BadRequest for invalid oversubscribed field when the value is less than zero" in {
      stubAuth()
      val invalidJsonBody: String =
        """
          |{
          |  "oversubscribed": -10,
          |  "traceAndMatch": 5,
          |  "failedEligibility": 3
          |}
    """.stripMargin

      val result = generateRequest(zRef = zRef, year = year, month = month, body = Json.parse(invalidJsonBody))

      result.status shouldBe BAD_REQUEST

      (result.json \ "code").as[String]    shouldBe "VALIDATION_FAILURE"
      (result.json \ "message").as[String] shouldBe "Bad request"

      val errors = (result.json \ "errors").as[Seq[JsValue]]
      (errors.head \ "code").as[String]    shouldBe "VALIDATION_ERROR"
      (errors.head \ "message").as[String] shouldBe "This field must be greater than or equal to 0"
      (errors.head \ "path").as[String]    shouldBe "/oversubscribed"

    }

    "return 400 BadRequest when validation fails for zRef" in {
      stubAuth(invalidZRef)
      val result = generateRequest(zRef = invalidZRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "INVALID_Z_REFERENCE"
      (result.json \ "message").as[String] shouldBe "Z reference is not formatted correctly"
    }

    "return 400 BadRequest when validation fails for taxYear" in {
      stubAuth()
      val result = generateRequest(zRef = zRef, year = invalidYear, month = month, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "INVALID_TAX_YEAR"
      (result.json \ "message").as[String] shouldBe "Tax year is not formatted correctly"
    }

    "return 400 BadRequest when validation fails for month" in {
      stubAuth()
      val result = generateRequest(zRef = zRef, year = year, month = invalidMonth, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "INVALID_MONTH"
      (result.json \ "message").as[String] shouldBe "Month is not formatted correctly"
    }

    "return 400 BadRequest when validation fails for month, tax year & zref" in {
      stubAuth(invalidZRef)
      val result = generateRequest(zRef = invalidZRef, year = invalidYear, month = invalidMonth, body = validParsedJson)

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "BAD_REQUEST"
      (result.json \ "message").as[String] shouldBe "Multiple issues found regarding your submission"

      val errors = (result.json \ "errors").as[Seq[JsValue]]
      errors.map(e => (e \ "code").as[String]) should contain allOf (
        "INVALID_Z_REFERENCE",
        "INVALID_TAX_YEAR",
        "INVALID_MONTH"
      )
    }

    "return 400 BadRequest when validation fails when an empty request body is submitted" in {
      stubAuth()
      stubGenerateReport(noContent, zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRawRequest(zRef = zRef, year = year, month = month, body = "")

      result.status                     shouldBe BAD_REQUEST
      (result.json \ "code").as[String] shouldBe "EMPTY_PAYLOAD"
      (result.json \ "message")
        .as[String] shouldBe "The payload is empty. Please ensure the request body contains a valid JSON payload before resubmitting."
    }

    "return 400 BadRequest when validation fails when a whitespace request body is submitted" in {
      stubAuth()
      stubGenerateReport(noContent, zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRawRequest(zRef = zRef, year = year, month = month, body = "   \n\t  ")

      result.status                     shouldBe BAD_REQUEST
      (result.json \ "code").as[String] shouldBe "EMPTY_PAYLOAD"
      (result.json \ "message")
        .as[String] shouldBe "The payload is empty. Please ensure the request body contains a valid JSON payload before resubmitting."
    }

    "return 400 BadRequest when malformed JSON is submitted" in {
      stubAuth()
      stubGenerateReport(noContent, zRef, year, month)
      stubCallback(noContent, zRef, year, month)

      val result = generateRawRequest(zRef = zRef, year = year, month = month, body = """{"oversubscribed": MALFORMED}""")

      result.status                        shouldBe BAD_REQUEST
      (result.json \ "code").as[String]    shouldBe "MALFORMED_JSON"
      (result.json \ "message").as[String] shouldBe "Request body contains malformed JSON"
    }

    "return 400 BadRequest when the record limit is exceeded" in {
      stubAuth()
      stubGenerateReport(
        badRequest(),
        zRef,
        year,
        month,
        Some("""{"code":"ISSUE_LIMIT_EXCEEDED","message":"You have exceeded the maximum allowed issues per reconciliation report"}""")
      )

      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status                     shouldBe BAD_REQUEST
      (result.json \ "code").as[String] shouldBe "ISSUE_LIMIT_EXCEEDED"
      (result.json \ "message").as[String] shouldBe
        s"The maximum number of issues that can be generated in a single report is ${appConfig.reportIssueLimit}. Please reduce the number of requested issues to be generated and try again."
    }

    "return 401 UNAUTHORIZED when zref doesn't match enrolment" in {
      stubAuth("11111")
      val result = generateRequest(zRef = zRef, year = year, month = month, body = validParsedJson)

      result.status                        shouldBe UNAUTHORIZED
      (result.json \ "code").as[String]    shouldBe "UNAUTHORIZED"
      (result.json \ "message").as[String] shouldBe "Z-Ref does not match enrolment."
    }
  }

  def generateRequest(
    zRef:  String,
    year:  String,
    month: String,
    body:  JsValue
  ): WSResponse =
    await(
      ws.url(
        s"http://localhost:$port/monthly/$zRef/$year/$month/reconciliation"
      ).withHttpHeaders("Authorization" -> "Bearer 1234")
        .withFollowRedirects(follow = false)
        .post(body)
    )

  def generateRawRequest(
    zRef:  String,
    year:  String,
    month: String,
    body:  String
  ): WSResponse =
    await(
      ws.url(
        s"http://localhost:$port/monthly/$zRef/$year/$month/reconciliation"
      ).withHttpHeaders(
        "Authorization" -> "Bearer 1234",
        "Content-Type"  -> "application/json"
      ).withFollowRedirects(follow = false)
        .post(body)
    )
}
