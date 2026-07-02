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

package models.errors

import play.api.libs.json.Json
import uk.gov.hmrc.disareturnstestsupportapi.models.errors._
import utils.BaseUnitSpec

class ErrorResponseSpec extends BaseUnitSpec {

  "ErrorResponse" should {
    "serialize InternalServerErr with default values" in {
      val err  = InternalServerErr()
      val json = Json.toJson(err)

      (json \ "code").as[String]    shouldBe "INTERNAL_SERVER_ERROR"
      (json \ "message").as[String] shouldBe "There has been an issue processing your request"
    }

    "deserialize InternalServerErr from JSON" in {
      val json = Json.obj(
        "code"    -> "INTERNAL_SERVER_ERROR",
        "message" -> "There has been an issue processing your request"
      )

      val result = json.as[InternalServerErr]
      result.code    shouldBe "INTERNAL_SERVER_ERROR"
      result.message shouldBe "There has been an issue processing your request"
    }

    "serialize ValidationFailureResponse correctly" in {
      val response = ValidationFailureResponse(
        errors = Seq(
          FieldValidationError("VALIDATION_ERROR", "ZReference did not match expected format", "/zRef"),
          FieldValidationError("VALIDATION_ERROR", "Month did not match expected format", "/month")
        )
      )

      val json = Json.toJson(response)
      (json \ "code").as[String]                           shouldBe "VALIDATION_FAILURE"
      (json \ "message").as[String]                        shouldBe "Bad request"
      (json \ "errors").as[Seq[FieldValidationError]].size shouldBe 2
    }

    "deserialize ValidationFailureResponse from JSON" in {
      val json = Json.obj(
        "code"    -> "VALIDATION_FAILURE",
        "message" -> "Bad request",
        "errors" -> Json.arr(
          Json.obj("code" -> "VALIDATION_ERROR", "message" -> "ZReference did not match expected format", "path" -> "/zRef"),
          Json.obj("code" -> "VALIDATION_ERROR", "message" -> "Month did not match expected format", "path"      -> "/month")
        )
      )

      val result = json.as[ValidationFailureResponse]
      result.code    shouldBe "VALIDATION_FAILURE"
      result.message shouldBe "Bad request"
      result.errors    should contain allOf (
        FieldValidationError("VALIDATION_ERROR", "ZReference did not match expected format", "/zRef"),
        FieldValidationError("VALIDATION_ERROR", "Month did not match expected format", "/month")
      )
    }
  }
}
