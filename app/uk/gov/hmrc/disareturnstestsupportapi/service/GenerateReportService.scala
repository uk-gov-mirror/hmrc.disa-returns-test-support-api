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

import play.api.Logging
import uk.gov.hmrc.disareturnstestsupportapi.connectors._
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import uk.gov.hmrc.disareturnstestsupportapi.models.errors._
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportService @Inject() (
  generateReportConnector: GenerateReportConnector,
  callbackConnector:       DisaReturnsCallbackConnector
)(implicit ec:             ExecutionContext)
    extends Logging {

  def generateReport(
    req:         GenerateReportRequest,
    zRef:        String,
    year:        String,
    month:       String
  )(implicit hc: HeaderCarrier): Future[GenerateReportResult] =
    generateReportConnector
      .generateReport(req, zRef, year, month)
      .flatMap {

        case GenerateReportResult.Success =>
          callbackConnector
            .callback(zRef, year, month, req.totalRecords)
            .map {
              case CallbackResponse.Success =>
                logger.info(
                  s"[GenerateReportService] Generate report successful zRef=$zRef year=$year month=$month"
                )
                GenerateReportResult.Success

              case _ =>
                logger.error(
                  s"[GenerateReportService] Callback failed zRef=$zRef year=$year month=$month"
                )
                GenerateReportResult.Failure
            }

        case GenerateReportResult.IssueLimitExceeded =>
          logger.warn(
            s"[GenerateReportService] Record limit exceeded zRef=$zRef year=$year month=$month"
          )
          Future.successful(GenerateReportResult.IssueLimitExceeded)

        case GenerateReportResult.Failure =>
          logger.error(
            s"[GenerateReportService] Generate report failed zRef=$zRef year=$year month=$month"
          )
          Future.successful(GenerateReportResult.Failure)
      }
}
