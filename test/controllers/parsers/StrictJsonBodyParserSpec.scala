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

package controllers.parsers

import org.apache.pekko.util.ByteString
import play.api.http.HeaderNames.CONTENT_TYPE
import play.api.http.MimeTypes.JSON
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.disareturnstestsupportapi.controllers.parsers.StrictJsonBodyParser
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.{EmptyPayload, MalformedJsonFailureErr}
import utils.BaseUnitSpec

import scala.concurrent.Future

class StrictJsonBodyParserSpec extends BaseUnitSpec {

  private val parser = new StrictJsonBodyParser()

  "StrictJsonBodyParser" should {

    "return BadRequest with EmptyPayload when request body is empty" in {
      val request = FakeRequest("POST", "/").withHeaders(CONTENT_TYPE -> JSON)
      val result  = await(parser(request).run())

      result.isLeft shouldBe true
      val badRequestResult = result.swap.getOrElse(fail("Expected Left(Result)"))
      status(Future.successful(badRequestResult))        shouldBe BAD_REQUEST
      contentAsJson(Future.successful(badRequestResult)) shouldBe Json.toJson(EmptyPayload())
    }

    "return BadRequest with EmptyPayload when request body is whitespace only" in {
      val request = FakeRequest("POST", "/").withHeaders(CONTENT_TYPE -> JSON)
      val result  = await(parser(request).run(ByteString("   \n\t  ")))

      result.isLeft shouldBe true
      val badRequestResult = result.swap.getOrElse(fail("Expected Left(Result)"))
      status(Future.successful(badRequestResult))        shouldBe BAD_REQUEST
      contentAsJson(Future.successful(badRequestResult)) shouldBe Json.toJson(EmptyPayload())
    }

    "return BadRequest with MalformedJsonFailureErr when request body is malformed JSON" in {
      val request = FakeRequest("POST", "/").withHeaders(CONTENT_TYPE -> JSON)
      val result  = await(parser(request).run(ByteString("""{"oversubscribed": MALFORMED}""")))

      result.isLeft shouldBe true
      val badRequestResult = result.swap.getOrElse(fail("Expected Left(Result)"))
      status(Future.successful(badRequestResult))        shouldBe BAD_REQUEST
      contentAsJson(Future.successful(badRequestResult)) shouldBe Json.toJson(MalformedJsonFailureErr())
    }

    "parse valid JSON into JsValue" in {
      val request = FakeRequest("POST", "/").withHeaders(CONTENT_TYPE -> JSON)
      val result  = await(parser(request).run(ByteString("""{"oversubscribed": 1}""")))

      result match {
        case Right(js) => (js \ "oversubscribed").as[Int] shouldBe 1
        case other     => fail(s"Expected Right(JsValue) but got $other")
      }
    }
  }
}
