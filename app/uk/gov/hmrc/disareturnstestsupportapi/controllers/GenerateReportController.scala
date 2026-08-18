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

package uk.gov.hmrc.disareturnstestsupportapi.controllers

import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.disareturnstestsupportapi.controllers.actions.AuthAction
import uk.gov.hmrc.disareturnstestsupportapi.controllers.parsers.StrictJsonBodyParser
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.validators._
import uk.gov.hmrc.disareturnstestsupportapi.models.errors._
import uk.gov.hmrc.disareturnstestsupportapi.service.GenerateReportService
import uk.gov.hmrc.disareturnstestsupportapi.utils.RequestParser
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GenerateReportController @Inject() (
  cc:                    ControllerComponents,
  authAction:            AuthAction,
  strictJsonBodyParser:  StrictJsonBodyParser,
  requestParser:         RequestParser,
  generateReportService: GenerateReportService,
  appConfig:             AppConfig
)(implicit ec:           ExecutionContext)
    extends AbstractController(cc)
    with Logging {

  def generateReport(zRef: String): Action[JsValue] =
    Action.async(strictJsonBodyParser) { implicit request =>
      implicit val hc: HeaderCarrier =
        HeaderCarrierConverter.fromRequest(request)
      requestParser.parseJson[GenerateReportRequest](request.body) match {
        case Left(errorResult) =>
          Future.successful(errorResult)

        case Right(req) =>
          validateZRef(zRef) match {
            case Left(errorResult) =>
              Future.successful(errorResult)

            case Right(validZRef) =>
              authAction(validZRef)
                .invokeBlock(
                  request,
                  (_: Request[JsValue]) =>
                    generateReportService
                      .generateReport(req, validZRef)
                      .map {
                        case GenerateReportResult.Success =>
                          NoContent
                        case GenerateReportResult.IssueLimitExceeded =>
                          BadRequest(Json.toJson(IssueLimitExceeded(appConfig.reportIssueLimit)))
                        case GenerateReportResult.Failure =>
                          InternalServerError(Json.toJson(InternalServerErr()))
                      }
                )
                .recover { case e =>
                  logger.error(
                    s"[GenerateReportController][generateReport] Unexpected error zRef=$zRef",
                    e
                  )
                  InternalServerError(Json.toJson(InternalServerErr()))
                }
          }
      }
    }

  private def validateZRef(zRef: String): Either[Result, String] =
    Either.cond(
      IsaRefValidator.isValid(zRef),
      zRef.toUpperCase,
      BadRequest(Json.toJson[ErrorResponse](InvalidZref))
    )
}
