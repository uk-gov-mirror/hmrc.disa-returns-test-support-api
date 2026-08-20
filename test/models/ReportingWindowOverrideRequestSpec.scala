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

package models

import play.api.libs.json.Json
import uk.gov.hmrc.disareturnstestsupportapi.models.ReportingWindowOverrideRequest
import utils.BaseUnitSpec
import utils.TestConstants.{reportingWindowEnd, reportingWindowStart}

class ReportingWindowOverrideRequestSpec extends BaseUnitSpec {

  "ReportingWindowOverrideRequest" should {
    "read RFC3339 timestamps with offsets" in {
      val result = Json
        .obj(
          "startDate" -> "2026-08-13T01:00:00+01:00",
          "endDate"   -> reportingWindowEnd.toString
        )
        .validate[ReportingWindowOverrideRequest]

      result.get shouldBe ReportingWindowOverrideRequest(
        reportingWindowStart,
        reportingWindowEnd
      )
    }

    "reject an invalid timestamp" in {
      Json
        .obj("startDate" -> "invalid", "endDate" -> reportingWindowEnd.toString)
        .validate[ReportingWindowOverrideRequest]
        .isError shouldBe true
    }

    "reject a start date after the end date" in {
      Json
        .obj("startDate" -> reportingWindowEnd.plusSeconds(1).toString, "endDate" -> reportingWindowEnd.toString)
        .validate[ReportingWindowOverrideRequest]
        .isError shouldBe true
    }
  }
}
