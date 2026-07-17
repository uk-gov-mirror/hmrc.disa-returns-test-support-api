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

package uk.gov.hmrc.disareturnstestsupportapi.models.validators

import play.api.libs.json.*
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.FieldValidationError

case class FailureResponseValidator(
  code:    String = "VALIDATION_FAILURE",
  message: String = "Bad request",
  errors:  Seq[FieldValidationError]
)

object FailureResponseValidator {
  implicit val responseFormat: OFormat[FailureResponseValidator] = Json.format[FailureResponseValidator]

  private def mapJsErrorToResponseCode(message: String): String = message match {
    case "error.path.missing" => "MISSING_FIELD"
    case _                    => "VALIDATION_ERROR"
  }

  private def formatFieldPath(jsPath: JsPath): String = {
    val pathString = jsPath.path
      .map {
        case KeyPathNode(key)     => s"/$key"
        case IdxPathNode(idx)     => s"/$idx"
        case RecursiveSearch(key) => s"//$key"
      }
      .mkString("")

    if (pathString.isEmpty) "/" else pathString
  }

  private def mapJsErrorMessage(message: String): String = message match {
    case "error.path.missing"      => "This field is required"
    case "error.min"               => "This field must be greater than or equal to 0"
    case "error.expected.jsnumber" => "This field must be greater than or equal to 0"
    case other                     => other
  }

  def createFromJsError(jsError: JsError): FailureResponseValidator = {
    val fieldErrors: Seq[FieldValidationError] = jsError.errors.toSeq.flatMap { case (path, errors) =>
      errors.map { validationError =>
        FieldValidationError(
          code = mapJsErrorToResponseCode(validationError.message),
          message = mapJsErrorMessage(validationError.message),
          path = formatFieldPath(path)
        )
      }
    }

    FailureResponseValidator(errors = fieldErrors)
  }

}
