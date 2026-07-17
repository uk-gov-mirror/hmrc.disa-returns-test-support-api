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

package uk.gov.hmrc.disareturnstestsupportapi.models.errors

import play.api.libs.json._

trait ErrorResponse {
  def code:    String
  def message: String
}

object ErrorResponse {
  implicit val writes: Writes[ErrorResponse] = Writes { error =>
    Json.obj(
      "code"    -> error.code,
      "message" -> error.message
    )
  }
}

case object InvalidZref extends ErrorResponse {
  val code    = "INVALID_Z_REFERENCE"
  val message = "Z reference is not formatted correctly"
}

case object InvalidTaxYear extends ErrorResponse {
  val code    = "INVALID_TAX_YEAR"
  val message = "Tax year is not formatted correctly"
}

case object InvalidMonth extends ErrorResponse {
  val code    = "INVALID_MONTH"
  val message = "Month is not formatted correctly"
}

case class EmptyPayload(
  code:    String = "EMPTY_PAYLOAD",
  message: String = "The payload is empty. Please ensure the request body contains a valid JSON payload before resubmitting."
)

object EmptyPayload {
  implicit val format: OFormat[EmptyPayload] = Json.format[EmptyPayload]
}

case class MalformedJsonFailureErr(
  code:    String = "MALFORMED_JSON",
  message: String = "Request body contains malformed JSON"
)

object MalformedJsonFailureErr {
  implicit val format: OFormat[MalformedJsonFailureErr] = Json.format[MalformedJsonFailureErr]
}

case class InternalServerErr(
  code:    String = "INTERNAL_SERVER_ERROR",
  message: String = "There has been an issue processing your request"
)

object InternalServerErr {
  implicit val format: OFormat[InternalServerErr] = Json.format[InternalServerErr]
}

case class IssueLimitExceeded(
  code:    String = "ISSUE_LIMIT_EXCEEDED",
  message: String
)

object IssueLimitExceeded {

  implicit val format: OFormat[IssueLimitExceeded] = Json.format[IssueLimitExceeded]

  def apply(limit: Int): IssueLimitExceeded =
    IssueLimitExceeded(
      message =
        s"The maximum number of issues that can be generated in a single report is $limit. Please reduce the number of requested issues to be generated and try again."
    )
}

case class UnauthorisedErr(code: String = "UNAUTHORIZED", message: String = "Unauthorized")

object UnauthorisedErr {
  implicit val format: OFormat[UnauthorisedErr] = Json.format[UnauthorisedErr]
}

case class MultipleErrorResponse(
  code:    String = "BAD_REQUEST",
  message: String = "Multiple issues found regarding your submission",
  errors:  Seq[ErrorResponse]
)

object MultipleErrorResponse {
  implicit val responseFormat: OWrites[MultipleErrorResponse] = Json.writes[MultipleErrorResponse]
}

case class FieldValidationError(
  code:    String,
  message: String,
  path:    String
)

object FieldValidationError {
  implicit val format: OFormat[FieldValidationError] = Json.format[FieldValidationError]
}
