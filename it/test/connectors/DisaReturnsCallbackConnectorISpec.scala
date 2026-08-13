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

package connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status.{BAD_REQUEST, SERVICE_UNAVAILABLE}
import play.api.test.Helpers.await
import uk.gov.hmrc.disareturnstestsupportapi.connectors.DisaReturnsCallbackConnector
import uk.gov.hmrc.disareturnstestsupportapi.models.callback.CallbackResponse
import utils.BaseIntegrationSpec

class DisaReturnsCallbackConnectorISpec extends BaseIntegrationSpec {

  private val zRef         = "Z1234"
  private val year         = "2025-26"
  private val month        = "FEB"
  private val callbackPath = s"/callback/monthly/$zRef/$year/$month"

  private lazy val connector = app.injector.instanceOf[DisaReturnsCallbackConnector]

  "DisaReturnsCallbackConnector" should {
    "make four requests for persistent 5xx responses" in {
      stubFor(post(urlEqualTo(callbackPath)).willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      await(connector.callback(zRef, year, month, 6)) shouldBe CallbackResponse.Failure
      verify(4, postRequestedFor(urlEqualTo(callbackPath)))
    }

    "make one request for a 4xx response" in {
      stubFor(post(urlEqualTo(callbackPath)).willReturn(aResponse().withStatus(BAD_REQUEST)))

      await(connector.callback(zRef, year, month, 6)) shouldBe CallbackResponse.Failure
      verify(1, postRequestedFor(urlEqualTo(callbackPath)))
    }
  }
}
