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
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST, INTERNAL_SERVER_ERROR, NO_CONTENT, SERVICE_UNAVAILABLE}
import uk.gov.hmrc.disareturnstestsupportapi.connectors.DisaReturnsCallbackConnector
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import utils.BaseUnitSpec

import scala.concurrent.Future

class DisaReturnsCallbackConnectorSpec extends BaseUnitSpec {

  trait TestSetup {
    clearInvocations(mockRequestBuilder)

    val retryConfig = ConfigFactory.parseString(
      "http-verbs.retries.intervals = [1 millisecond, 1 millisecond, 1 millisecond]"
    )
    val connector = new DisaReturnsCallbackConnector(mockAppConfig, mockHttpClient, retryConfig, system)

    val zref         = "Z1234"
    val year         = "2025-26"
    val month        = "FEB"
    val totalRecords = 42
    val testUrl      = "http://localhost:1200"

    when(mockAppConfig.disaReturnsBaseUrl).thenReturn(testUrl)
    when(mockHttpClient.post(url"$testUrl/callback/monthly/$zref/$year/$month")).thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }

  "CallbackConnector.sendMonthlyCallback" should {

    s"return Success when the response status is $NO_CONTENT" in new TestSetup {
      val httpResponse: HttpResponse = HttpResponse(NO_CONTENT, "")
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Right(httpResponse)))

      val result: CallbackResponse = connector.callback(zref, year, month, totalRecords).futureValue
      result shouldBe CallbackResponse.Success
    }

    Seq(INTERNAL_SERVER_ERROR, BAD_GATEWAY, SERVICE_UNAVAILABLE).foreach { status =>
      s"retry three times and return Failure when the response status is $status" in new TestSetup {
        val error = UpstreamErrorResponse("downstream unavailable", status)
        when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
          .thenReturn(Future.successful(Left(error)))

        connector.callback(zref, year, month, totalRecords).futureValue shouldBe CallbackResponse.Failure
        verify(mockRequestBuilder, times(4)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
      }
    }

    "not retry a non-5xx response" in new TestSetup {
      val error = UpstreamErrorResponse("bad request", BAD_REQUEST)
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.successful(Left(error)))

      connector.callback(zref, year, month, totalRecords).futureValue shouldBe CallbackResponse.Failure
      verify(mockRequestBuilder, times(1)).execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any())
    }

    "return Failure when the call throws an exception" in new TestSetup {
      when(mockRequestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]](any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Timeout")))

      val result: CallbackResponse = connector
        .callback(zref, year, month, totalRecords)
        .recover { case _ =>
          CallbackResponse.Failure
        }
        .futureValue

      result shouldBe CallbackResponse.Failure
    }
  }
}
