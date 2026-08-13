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

package uk.gov.hmrc.disareturnstestsupportapi.connectors

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.{CallbackRequest, CallbackResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DisaReturnsCallbackConnector @Inject() (
  config:                     AppConfig,
  httpClient:                 HttpClientV2,
  override val configuration: Config,
  override val actorSystem:   ActorSystem
)(implicit ec:                ExecutionContext)
    extends BaseConnector {

  def callback(zRef: String, year: String, month: String, totalRecords: Int)(implicit hc: HeaderCarrier): Future[CallbackResponse] = {
    val url  = url"${config.disaReturnsBaseUrl}/callback/monthly/$zRef/$year/$month"
    val body = CallbackRequest(totalRecords)
    retryFor[HttpResponse]("send DISA returns callback")(retryCondition) {
      httpClient
        .post(url)
        .withBody(Json.toJson(body))
        .executeOrFail
    }
      .map { response =>
        response.status match {
          case 204 => CallbackResponse.Success
          case _   => CallbackResponse.Failure
        }
      }
      .recover { case _: UpstreamErrorResponse => CallbackResponse.Failure }
  }
}
