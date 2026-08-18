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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, UNAUTHORIZED}
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers.{POST, contentAsJson, status}
import uk.gov.hmrc.disareturnstestsupportapi.controllers.GenerateReportController
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import utils.BaseUnitSpec

import scala.concurrent.Future

class GenerateReportControllerSpec extends BaseUnitSpec {

  private val controller = app.injector.instanceOf[GenerateReportController]

  val zRef = "Z1234"

  val validRequest: GenerateReportRequest = GenerateReportRequest(oversubscribed = 5, traceAndMatch = 20, failedEligibility = 6)
  val validJson:    JsValue               = Json.toJson(validRequest)

  "GenerateReportController.generateReport" should {

    "return 204 NoContent when both generateReport and callback succeed" in {
      authorizationForZRef()
      when(mockGenerateReportConnector.generateReport(any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Success))
      when(mockDisaReturnsCallbackConnector.callback(any(), any())(any()))
        .thenReturn(Future.successful(CallbackResponse.Success))

      val request = FakeRequest(POST, s"/monthly/$zRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef)(request)

      status(result) shouldBe NO_CONTENT
    }

    "return 401 Unauthorised when enrolment zRef doesn't match request zref" in {
      authorizationForZRef("Z2222")
      when(mockGenerateReportConnector.generateReport(any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Success))
      when(mockDisaReturnsCallbackConnector.callback(any(), any())(any()))
        .thenReturn(Future.successful(CallbackResponse.Success))

      val request = FakeRequest(POST, s"/monthly/$zRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef)(request)

      status(result) shouldBe UNAUTHORIZED
    }

    "return 500 InternalServerError when generateReport fails" in {
      authorizationForZRef()
      when(mockGenerateReportConnector.generateReport(any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Failure))

      val request = FakeRequest(POST, s"/monthly/$zRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef)(request)

      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 500 InternalServerError when callback fails" in {
      authorizationForZRef()
      when(mockGenerateReportConnector.generateReport(any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.Success))
      when(mockDisaReturnsCallbackConnector.callback(any(), any())(any()))
        .thenReturn(Future.successful(CallbackResponse.Failure))

      val request = FakeRequest(POST, s"/monthly/$zRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef)(request)

      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "return 400 BadRequest when zRef is invalid" in {
      val invalidzRef = "z123333333"
      authorizationForZRef(invalidzRef)

      val request = FakeRequest(POST, s"/monthly/$invalidzRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(invalidzRef)(request)

      status(result) shouldBe BAD_REQUEST
      val json = contentAsJson(result)
      (json \ "code").as[String]    shouldBe "INVALID_Z_REFERENCE"
      (json \ "message").as[String] shouldBe "Z reference is not formatted correctly"
    }

    "return 400 BadRequest when the issue limit is exceeded" in {
      authorizationForZRef()

      when(mockGenerateReportConnector.generateReport(any(), any())(any()))
        .thenReturn(Future.successful(GenerateReportResult.IssueLimitExceeded))

      val request = FakeRequest(POST, s"/monthly/$zRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef)(request)

      status(result) shouldBe BAD_REQUEST

      val json = contentAsJson(result)
      (json \ "code").as[String] shouldBe "ISSUE_LIMIT_EXCEEDED"
      (json \ "message").as[String] should include(
        s"The maximum number of issues that can be generated in a single report is ${mockAppConfig.reportIssueLimit}. Please reduce the number of requested issues to be generated and try again."
      )
    }

    "return 500 InternalServerError when generateReport throws an exception (recover block)" in {
      authorizationForZRef()
      when(mockGenerateReportConnector.generateReport(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("fail")))

      val request = FakeRequest(POST, s"/monthly/$zRef/reconciliation")
        .withBody(validJson)
        .withHeaders("Content-Type" -> "application/json")

      val result = controller.generateReport(zRef)(request)

      status(result)                                 shouldBe INTERNAL_SERVER_ERROR
      (contentAsJson(result) \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (contentAsJson(result) \ "message").as[String] shouldBe "There has been an issue processing your request"
    }
  }

}
