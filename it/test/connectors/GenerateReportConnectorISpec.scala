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
import com.github.tomakehurst.wiremock.stubbing.Scenario
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, SERVICE_UNAVAILABLE}
import play.api.test.Helpers.await
import uk.gov.hmrc.disareturnstestsupportapi.connectors.GenerateReportConnector
import uk.gov.hmrc.disareturnstestsupportapi.models.GenerateReportRequest
import uk.gov.hmrc.disareturnstestsupportapi.models.errors.GenerateReportResult
import utils.BaseIntegrationSpec
import utils.TestConstants.validZReference

class GenerateReportConnectorISpec extends BaseIntegrationSpec {

  private val zRef       = validZReference
  private val reportPath = s"/monthly/$zRef/reconciliation"
  private val reportBody = GenerateReportRequest(oversubscribed = 1, traceAndMatch = 2, failedEligibility = 3)

  private lazy val connector = app.injector.instanceOf[GenerateReportConnector]

  "GenerateReportConnector" should {
    "make four requests for persistent 5xx responses" in {
      stubFor(post(urlEqualTo(reportPath)).willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE)))

      await(connector.generateReport(reportBody, zRef)) shouldBe GenerateReportResult.Failure
      verify(4, postRequestedFor(urlEqualTo(reportPath)))
    }

    "recover after a transient 5xx response" in {
      stubFor(
        post(urlEqualTo(reportPath))
          .inScenario("report recovers")
          .whenScenarioStateIs(Scenario.STARTED)
          .willReturn(aResponse().withStatus(SERVICE_UNAVAILABLE))
          .willSetStateTo("recovered")
      )
      stubFor(
        post(urlEqualTo(reportPath))
          .inScenario("report recovers")
          .whenScenarioStateIs("recovered")
          .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      await(connector.generateReport(reportBody, zRef)) shouldBe GenerateReportResult.Success
      verify(2, postRequestedFor(urlEqualTo(reportPath)))
    }

    "make one request for a 4xx response" in {
      stubFor(post(urlEqualTo(reportPath)).willReturn(aResponse().withStatus(BAD_REQUEST).withBody("{}")))

      await(connector.generateReport(reportBody, zRef)) shouldBe GenerateReportResult.Failure
      verify(1, postRequestedFor(urlEqualTo(reportPath)))
    }
  }
}
