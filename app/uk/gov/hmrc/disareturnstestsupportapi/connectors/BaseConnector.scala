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

import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.RequestBuilder
import uk.gov.hmrc.http.{HttpResponse, Retries, UpstreamErrorResponse}

import scala.concurrent.{ExecutionContext, Future}

trait BaseConnector extends Retries {

  extension (requestBuilder: RequestBuilder)
    protected def executeOrFail(implicit ec: ExecutionContext): Future[HttpResponse] =
      requestBuilder.execute[Either[UpstreamErrorResponse, HttpResponse]].flatMap {
        case Left(error)     => Future.failed(error)
        case Right(response) => Future.successful(response)
      }

  protected def retryCondition: PartialFunction[Exception, Boolean] = { case UpstreamErrorResponse.Upstream5xxResponse(_) =>
    true
  }
}
