/*
 * Copyright 2026 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{never, verify, when}
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes.JSON
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, PUT, status}
import uk.gov.hmrc.disareturnstestsupportapi.controllers.ReportingWindowOverrideController
import uk.gov.hmrc.disareturnstestsupportapi.models.ReportingWindowOverrideRequest
import utils.BaseUnitSpec
import utils.TestConstants.{reportingWindowEnd, reportingWindowStart, validZReference}

import scala.concurrent.Future

class ReportingWindowOverrideControllerSpec extends BaseUnitSpec {

  private val controller = app.injector.instanceOf[ReportingWindowOverrideController]
  private val requestBody = Json.obj(
    "startDate" -> reportingWindowStart.toString,
    "endDate"   -> reportingWindowEnd.toString
  )

  "ReportingWindowOverrideController.set" should {
    "store the override using the validated uppercase Z-reference" in {
      authorizationForZRef()
      when(mockReportingWindowOverrideConnector.setOverride(any(), eqTo(validZReference))(any()))
        .thenReturn(Future.successful(true))

      val result = controller.set("z1234")(
        FakeRequest(PUT, "/monthly/z1234/reporting-window-override")
          .withBody(requestBody)
          .withHeaders(CONTENT_TYPE -> JSON)
      )

      status(result) shouldBe NO_CONTENT
      verify(mockReportingWindowOverrideConnector).setOverride(
        eqTo(
          ReportingWindowOverrideRequest(
            reportingWindowStart,
            reportingWindowEnd
          )
        ),
        eqTo(validZReference)
      )(any())
    }

    "reject an invalid date without updating the override" in {
      val result = controller.set(validZReference)(
        FakeRequest(PUT, s"/monthly/$validZReference/reporting-window-override")
          .withJsonBody(requestBody ++ Json.obj("startDate" -> "invalid"))
      )

      status(result) shouldBe BAD_REQUEST
      verify(mockReportingWindowOverrideConnector, never()).setOverride(any(), any())(any())
    }

    "reject a start date after the end date without updating the override" in {
      val result = controller.set(validZReference)(
        FakeRequest(PUT, s"/monthly/$validZReference/reporting-window-override").withJsonBody(
          Json.obj(
            "startDate" -> "2026-09-01T00:00:00Z",
            "endDate"   -> reportingWindowEnd.toString
          )
        )
      )

      status(result) shouldBe BAD_REQUEST
      verify(mockReportingWindowOverrideConnector, never()).setOverride(any(), any())(any())
    }

    "reject an invalid Z-reference without updating the override" in {
      val result = controller.set("invalid")(
        FakeRequest(PUT, "/monthly/invalid/reporting-window-override")
          .withBody(requestBody)
          .withHeaders(CONTENT_TYPE -> JSON)
      )

      status(result) shouldBe BAD_REQUEST
      verify(mockReportingWindowOverrideConnector, never()).setOverride(any(), any())(any())
    }

    "return an internal server error when the stub rejects the update" in {
      authorizationForZRef()
      when(mockReportingWindowOverrideConnector.setOverride(any(), any())(any()))
        .thenReturn(Future.successful(false))

      val result = controller.set(validZReference)(
        FakeRequest(PUT, s"/monthly/$validZReference/reporting-window-override")
          .withBody(requestBody)
          .withHeaders(CONTENT_TYPE -> JSON)
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return an internal server error when setting the override fails unexpectedly" in {
      authorizationForZRef()
      when(mockReportingWindowOverrideConnector.setOverride(any(), any())(any()))
        .thenReturn(Future.failed(new RuntimeException("unexpected failure")))

      val result = controller.set(validZReference)(
        FakeRequest(PUT, s"/monthly/$validZReference/reporting-window-override")
          .withBody(requestBody)
          .withHeaders(CONTENT_TYPE -> JSON)
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
