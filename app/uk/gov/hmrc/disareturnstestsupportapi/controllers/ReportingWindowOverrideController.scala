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

package uk.gov.hmrc.disareturnstestsupportapi.controllers

import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import uk.gov.hmrc.disareturnstestsupportapi.connectors.ReportingWindowOverrideConnector
import uk.gov.hmrc.disareturnstestsupportapi.controllers.actions.{AuthAction, AuthenticatedRequest}
import uk.gov.hmrc.disareturnstestsupportapi.controllers.parsers.StrictJsonBodyParser
import uk.gov.hmrc.disareturnstestsupportapi.models.ReportingWindowOverrideRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.{ErrorResponse, InternalServerErr, InvalidZref}
import uk.gov.hmrc.disareturnstestsupportapi.models.validators.IsaRefValidator
import uk.gov.hmrc.disareturnstestsupportapi.utils.RequestParser
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingWindowOverrideController @Inject() (
  cc:                               ControllerComponents,
  authAction:                       AuthAction,
  strictJsonBodyParser:             StrictJsonBodyParser,
  requestParser:                    RequestParser,
  reportingWindowOverrideConnector: ReportingWindowOverrideConnector
)(implicit ec:                      ExecutionContext)
    extends AbstractController(cc) {

  def set(zRef: String): Action[JsValue] = Action.async(strictJsonBodyParser) { implicit request =>
    requestParser.parseJson[ReportingWindowOverrideRequest](request.body) match {
      case Left(errorResult) => Future.successful(errorResult)
      case Right(overrideRequest) if !IsaRefValidator.isValid(zRef) =>
        Future.successful(BadRequest(Json.toJson[ErrorResponse](InvalidZref)))
      case Right(overrideRequest) =>
        val validZRef = zRef.toUpperCase
        authAction(validZRef)
          .invokeBlock(
            request,
            (authenticatedRequest: AuthenticatedRequest[JsValue]) => {
              implicit val hc: HeaderCarrier = HeaderCarrierConverter.fromRequest(authenticatedRequest)
              reportingWindowOverrideConnector
                .setOverride(overrideRequest, authenticatedRequest.credId)
                .map {
                  case true  => NoContent
                  case false => InternalServerError(Json.toJson(InternalServerErr()))
                }
            }
          )
    }
  }
}
