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

package utils

import play.api.libs.json.*
import play.api.test.Helpers.contentAsJson
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.validators.FailureResponseValidator
import uk.gov.hmrc.disareturnstestsupportapi.utils.RequestParser

import scala.concurrent.Future

class RequestParserSpec extends BaseUnitSpec {
  val parsers = new RequestParser()

  "CustomParsers#parseJsonOrEmpty" should {

    "return Left(FailureResponseValidator) when the request body is invalid JSON" in {
      val invalidJson = """{"oversubscribed": 1, "traceAndMatch": "NotInt", "failedEligibility": 1 }"""

      val result = parsers.parseJson[GenerateReportRequest](Json.parse(invalidJson))

      result.isLeft shouldBe true

      val leftResult = result.swap.getOrElse(fail("Expected Left(Result)"))
      leftResult.header.status shouldBe 400

      val bodyJson           = contentAsJson(Future.successful(leftResult))
      val validationResponse = bodyJson.validate[FailureResponseValidator]

      validationResponse.isSuccess shouldBe true

      val response = validationResponse.get
      response.code            shouldBe "VALIDATION_FAILURE"
      response.errors.nonEmpty shouldBe true

      response.errors.map(_.path) should contain("/traceAndMatch")
    }

    "return Right(TestPayload) when the request body is valid JSON" in {
      val validJson = Json.obj("oversubscribed" -> 1, "traceAndMatch" -> 1, "failedEligibility" -> 1)

      val result = parsers.parseJson[GenerateReportRequest](validJson)
      result.isRight shouldBe true

      result.toOption.get shouldBe GenerateReportRequest(oversubscribed = 1, traceAndMatch = 1, failedEligibility = 1)
    }
  }
}
