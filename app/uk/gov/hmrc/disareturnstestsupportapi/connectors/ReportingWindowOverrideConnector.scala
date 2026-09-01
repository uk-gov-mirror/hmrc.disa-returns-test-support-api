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

package uk.gov.hmrc.disareturnstestsupportapi.connectors

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.disareturnstestsupportapi.models.ReportingWindowOverrideRequest
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReportingWindowOverrideConnector @Inject() (
  config:                     AppConfig,
  httpClient:                 HttpClientV2,
  override val configuration: Config,
  override val actorSystem:   ActorSystem
)(implicit ec:                ExecutionContext)
    extends BaseConnector
    with Logging {

  def setOverride(body: ReportingWindowOverrideRequest, zRef: String)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val url = url"${config.disaReturnsStubsBaseUrl}/reporting-window-override/$zRef"

    retryFor[HttpResponse]("set reporting window override")(retryCondition) {
      httpClient
        .put(url)
        .withBody(Json.toJson(body))
        .executeOrFail
    }.map(_.status == NO_CONTENT)
      .recover { case error: UpstreamErrorResponse =>
        logger.error(s"[ReportingWindowOverrideConnector][setOverride] Error setting reporting window override: ${error.message}")
        false
      }
  }
}
