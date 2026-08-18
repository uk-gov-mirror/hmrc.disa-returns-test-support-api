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
import play.api.Logging
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT}
import play.api.libs.json.Json
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class GenerateReportConnector @Inject() (
  config:                     AppConfig,
  httpClient:                 HttpClientV2,
  override val configuration: Config,
  override val actorSystem:   ActorSystem
)(implicit ec:                ExecutionContext)
    extends BaseConnector
    with Logging {

  def generateReport(
    body:        GenerateReportRequest,
    zRef:        String
  )(implicit hc: HeaderCarrier): Future[GenerateReportResult] = {

    val url = url"${config.disaReturnsStubsBaseUrl}/test-only/$zRef/reconciliation"
    retryFor[HttpResponse]("generate reconciliation report")(retryCondition) {
      httpClient
        .post(url)
        .withBody(Json.toJson(body))
        .setHeader("Authorization" -> s"Bearer")
        .executeOrFail
    }
      .map { response =>
        resultFor(response.status, response.body)
      }
      .recover { case error: UpstreamErrorResponse =>
        logger.error(s"[GenerateReportConnector][generateReport] Error generating reconciliation report: ${error.message}", error.getCause)
        resultFor(error.statusCode, responseBody(error))
      }
  }

  private def resultFor(status: Int, body: String): GenerateReportResult =
    status match {
      case NO_CONTENT => GenerateReportResult.Success
      case BAD_REQUEST =>
        val code = Try((Json.parse(body) \ "code").asOpt[String]).toOption.flatten
        code match {
          case Some("ISSUE_LIMIT_EXCEEDED") => GenerateReportResult.IssueLimitExceeded
          case _                            => GenerateReportResult.Failure
        }
      case _ => GenerateReportResult.Failure
    }

  private def responseBody(error: UpstreamErrorResponse): String = {
    val bodyPattern = "(?s).*Response body: '(.*)'$".r
    bodyPattern.findFirstMatchIn(error.message).map(_.group(1)).getOrElse(error.message)
  }
}
