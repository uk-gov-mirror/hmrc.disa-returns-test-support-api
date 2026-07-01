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

package uk.gov.hmrc.disareturnstestsupportapi.controllers.parsers

import org.apache.pekko.stream.scaladsl.Sink
import org.apache.pekko.util.ByteString
import play.api.Logging
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.Results.BadRequest
import play.api.mvc._
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.{EmptyPayload, MalformedJsonFailureErr}

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import scala.util.control.NonFatal

@Singleton
class StrictJsonBodyParser @Inject() ()(implicit ec: ExecutionContext) extends BodyParser[JsValue] with Logging {

  override def apply(request: RequestHeader): Accumulator[ByteString, Either[Result, JsValue]] =
    Accumulator(Sink.fold[ByteString, ByteString](ByteString.empty)(_ ++ _)).map { bytes =>
      if (bytes.utf8String.trim.isEmpty)
        Left(BadRequest(Json.toJson(EmptyPayload())))
      else
        try Right(Json.parse(bytes.toArray))
        catch {
          case NonFatal(e) =>
            logger.warn("Request body contains malformed JSON", e)
            Left(BadRequest(Json.toJson(MalformedJsonFailureErr())))
        }
    }
}
