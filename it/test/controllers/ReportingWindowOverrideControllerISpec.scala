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

package controllers

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.libs.json.Json
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import play.api.test.Helpers.await
import utils.BaseIntegrationSpec
import utils.TestConstants.{bearerToken, reportingWindowEnd, reportingWindowStart, validZReference}

class ReportingWindowOverrideControllerISpec extends BaseIntegrationSpec {

  "PUT /monthly/:zRef/reporting-window-override" should {
    "forward a valid override using the uppercase Z-reference" in {
      stubAuth()
      stubReportingWindowOverride(aResponse().withStatus(204), validZReference)

      val response = await(
        ws.url(s"http://localhost:$port/monthly/${validZReference.toLowerCase}/reporting-window-override")
          .withHttpHeaders(AUTHORIZATION -> bearerToken)
          .put(
            Json.obj(
              "startDate" -> reportingWindowStart.toString,
              "endDate"   -> reportingWindowEnd.toString
            )
          )
      )

      response.status shouldBe 204
    }

    "reject an invalid interval without calling the stub" in {
      val response = await(
        ws.url(s"http://localhost:$port/monthly/$validZReference/reporting-window-override")
          .withHttpHeaders(AUTHORIZATION -> bearerToken)
          .put(
            Json.obj(
              "startDate" -> reportingWindowEnd.plusSeconds(1).toString,
              "endDate"   -> reportingWindowEnd.toString
            )
          )
      )

      response.status shouldBe 400
    }
  }
}
