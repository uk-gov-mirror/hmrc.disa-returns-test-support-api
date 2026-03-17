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

package utils

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder
import com.github.tomakehurst.wiremock.client.WireMock._
import play.api.http.Status
import play.api.http.Status.OK

trait CommonStubs {

  def stubAuth(zRef: String = "Z1234"): Unit = {
    val validEnrolment =
      s"""{
      "authorisedEnrolments": [{
        "key": "HMRC-DISA-ORG",
        "identifiers": [{ "key": "ZREF", "value": "$zRef" }],
        "state": "Activated"
      }]
      }"""

    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse.withStatus(OK).withBody(validEnrolment)
        }
    }
  }

  def stubAuthFail(): Unit =
    stubFor {
      post("/auth/authorise")
        .willReturn {
          aResponse()
            .withStatus(Status.UNAUTHORIZED)
            .withBody("{}")
        }
    }

  def stubGenerateReport(
    status:       ResponseDefinitionBuilder,
    zRef:         String,
    year:         String,
    month:        String,
    responseBody: Option[String] = None
  ): Unit = {

    val responseWithBody = responseBody match {
      case Some(body) => status.withBody(body)
      case None       => status
    }

    stubFor(
      post(urlEqualTo(s"/test-only/$zRef/$year/$month/reconciliation"))
        .willReturn(responseWithBody)
    )
  }

  def stubCallback(status: ResponseDefinitionBuilder, zRef: String, year: String, month: String): Unit =
    stubFor(
      post(urlEqualTo(s"/callback/monthly/$zRef/$year/$month"))
        .willReturn(status)
    )

}
