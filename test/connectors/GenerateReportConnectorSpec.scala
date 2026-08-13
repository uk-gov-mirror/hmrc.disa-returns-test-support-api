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

package connectors

import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.disareturnstestsupportapi.connectors.GenerateReportConnector
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class GenerateReportConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    clearInvocations(mockRequestBuilder)

    val retryConfig = ConfigFactory.parseString(
      "http-verbs.retries.intervals = [1 millisecond, 1 millisecond, 1 millisecond]"
    )
    val connector = new GenerateReportConnector(mockAppConfig, mockHttpClient, retryConfig, system)

    val zref  = "Z1234"
    val year  = "2025-26"
    val month = "JAN"
    val body: GenerateReportRequest = GenerateReportRequest(oversubscribed = 10, traceAndMatch = 20, failedEligibility = 12)
    val testUrl = "http://localhost:1204"

    when(mockAppConfig.disaReturnsStubsBaseUrl).thenReturn(testUrl)
    when(mockHttpClient.post(url"$testUrl/test-only/$zref/$year/$month/reconciliation"))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.setHeader(any()))
      .thenReturn(mockRequestBuilder)
  }

  "GenerateReportConnector.generateReport" should {

    "return Success when the response status is 204" in new TestSetup {
      val httpResponse: HttpResponse = HttpResponse(204, "")
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(httpResponse)))

      val result: GenerateReportResult = connector.generateReport(body, zref, year, month).futureValue
      result shouldBe GenerateReportResult.Success
    }

    Seq(INTERNAL_SERVER_ERROR, 502, 503).foreach { status =>
      s"retry three times and return Failure when the response status is $status" in new TestSetup {
        val error = UpstreamErrorResponse("downstream unavailable", status)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(error)))

        connector.generateReport(body, zref, year, month).futureValue shouldBe GenerateReportResult.Failure
        verify(mockRequestBuilder, times(4)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "return Failure when the call throws an exception" in new TestSetup {
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Timeout")))
      val result: GenerateReportResult = connector
        .generateReport(body, zref, year, month)
        .recover { case _ =>
          GenerateReportResult.Failure
        }
        .futureValue

      result shouldBe GenerateReportResult.Failure
    }

    "return IssueLimitExceeded when the response status is 400 with code ISSUE_LIMIT_EXCEEDED" in new TestSetup {
      val responseBody: String =
        s"""
          |{
          |  "code": "ISSUE_LIMIT_EXCEEDED",
          |  "message": "Maximum of ${mockAppConfig.reportIssueLimit} issues allowed per report"
          |}
          |""".stripMargin

      val error = UpstreamErrorResponse(s"POST returned 400. Response body: '$responseBody'", 400)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Left(error)))

      val result: GenerateReportResult = connector.generateReport(body, zref, year, month).futureValue
      result shouldBe GenerateReportResult.IssueLimitExceeded
      verify(mockRequestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
    }
  }
}
