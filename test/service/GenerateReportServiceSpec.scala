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

package uk.gov.hmrc.disareturnstestsupportapi.service

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import uk.gov.hmrc.http.HeaderCarrier
import utils.BaseUnitSpec

import scala.concurrent.Future

class GenerateReportServiceSpec extends BaseUnitSpec {

  private val service = new GenerateReportService(
    mockGenerateReportConnector,
    mockDisaReturnsCallbackConnector
  )

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockGenerateReportConnector, mockDisaReturnsCallbackConnector)
  }

  private val request =
    GenerateReportRequest(oversubscribed = 1, traceAndMatch = 1, failedEligibility = 1)

  private val zRef  = "Z123456"
  private val year  = "2024"
  private val month = "01"

  "GenerateReportService#generateReport" should {

    "return Success when generate succeeds and callback succeeds" in {
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.successful(GenerateReportResult.Success))

      when(mockDisaReturnsCallbackConnector.callback(zRef, year, month, request.totalRecords))
        .thenReturn(Future.successful(CallbackResponse.Success))

      val result = service.generateReport(request, zRef, year, month).futureValue

      result shouldBe GenerateReportResult.Success
    }

    "return Failure when generate succeeds but callback fails" in {
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.successful(GenerateReportResult.Success))

      when(mockDisaReturnsCallbackConnector.callback(zRef, year, month, request.totalRecords))
        .thenReturn(Future.successful(CallbackResponse.Failure))

      val result = service.generateReport(request, zRef, year, month).futureValue

      result shouldBe GenerateReportResult.Failure
    }

    "return Failure when generate fails" in {
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.successful(GenerateReportResult.Failure))

      val result = service.generateReport(request, zRef, year, month).futureValue

      result shouldBe GenerateReportResult.Failure

      verify(mockDisaReturnsCallbackConnector, never())
        .callback(any[String], any[String], any[String], any[Int])(any[HeaderCarrier])
    }

    "propagate exception if generate connector throws" in {
      val ex = new RuntimeException("Boom!")
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.failed(ex))

      val thrown = service.generateReport(request, zRef, year, month).failed.futureValue

      thrown shouldBe ex
      verify(mockDisaReturnsCallbackConnector, never())
        .callback(any[String], any[String], any[String], any[Int])(any[HeaderCarrier])
    }

    "propagate exception if callback connector throws" in {
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.successful(GenerateReportResult.Success))

      val ex = new RuntimeException("Callback boom!")
      when(mockDisaReturnsCallbackConnector.callback(zRef, year, month, request.totalRecords))
        .thenReturn(Future.failed(ex))

      val thrown = service.generateReport(request, zRef, year, month).failed.futureValue

      thrown shouldBe ex
    }

    "return IssueLimitExceeded when generate reports hits the record limit" in {
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.successful(GenerateReportResult.IssueLimitExceeded))

      val result = service.generateReport(request, zRef, year, month).futureValue

      result shouldBe GenerateReportResult.IssueLimitExceeded

      verify(mockDisaReturnsCallbackConnector, never())
        .callback(any[String], any[String], any[String], any[Int])(any[HeaderCarrier])
    }

    "return IssueLimitExceeded even if callback connector would have thrown" in {
      when(mockGenerateReportConnector.generateReport(request, zRef, year, month))
        .thenReturn(Future.successful(GenerateReportResult.IssueLimitExceeded))

      val ex = new RuntimeException("Callback boom!")
      when(mockDisaReturnsCallbackConnector.callback(zRef, year, month, request.totalRecords))
        .thenReturn(Future.failed(ex))

      val result = service.generateReport(request, zRef, year, month).futureValue

      result shouldBe GenerateReportResult.IssueLimitExceeded

      verify(mockDisaReturnsCallbackConnector, never())
        .callback(any[String], any[String], any[String], any[Int])(any[HeaderCarrier])
    }
  }
}
