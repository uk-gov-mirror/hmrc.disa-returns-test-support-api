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

package connectors

import com.typesafe.config.ConfigFactory
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify, when}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import uk.gov.hmrc.disareturnstestsupportapi.connectors.ReportingWindowOverrideConnector
import uk.gov.hmrc.disareturnstestsupportapi.models.ReportingWindowOverrideRequest
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec
import utils.TestConstants.{reportingWindowEnd, reportingWindowStart, testCredentialId}

import scala.concurrent.Future

class ReportingWindowOverrideConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    private val retryConfig = ConfigFactory.parseString(
      "http-verbs.retries.intervals = [1 millisecond, 1 millisecond, 1 millisecond]"
    )
    private val baseUrl = "http://localhost:1204"

    val connector = new ReportingWindowOverrideConnector(mockAppConfig, mockHttpClient, retryConfig, system)
    val request   = ReportingWindowOverrideRequest(reportingWindowStart, reportingWindowEnd)

    when(mockAppConfig.disaReturnsStubsBaseUrl).thenReturn(baseUrl)
    when(mockHttpClient.put(url"$baseUrl/reporting-window-override")).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader("X-Cred-Id" -> testCredentialId)).thenReturn(mockRequestBuilder)
  }

  "ReportingWindowOverrideConnector.setOverride" should {
    "return true when the stub accepts the override" in new TestSetup {
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(NO_CONTENT, ""))))

      connector.setOverride(request, testCredentialId).futureValue shouldBe true
    }

    "return false when the stub returns another successful status" in new TestSetup {
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(HttpResponse(OK, ""))))

      connector.setOverride(request, testCredentialId).futureValue shouldBe false
    }

    "retry server errors and return false" in new TestSetup {
      val error = UpstreamErrorResponse("downstream unavailable", INTERNAL_SERVER_ERROR)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Left(error)))

      connector.setOverride(request, testCredentialId).futureValue shouldBe false
      verify(mockRequestBuilder, times(4)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
    }
  }
}
