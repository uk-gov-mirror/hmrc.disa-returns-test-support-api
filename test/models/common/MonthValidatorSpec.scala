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

package models.common

import uk.gov.hmrc.disareturnstestsupportapi.models.validators.MonthValidator
import utils.BaseUnitSpec

class MonthValidatorSpec extends BaseUnitSpec {

  "MonthValidator.isValid" should {

    "return true for valid uppercase month abbreviations" in {
      MonthValidator.isValid("JAN") shouldBe true
      MonthValidator.isValid("FEB") shouldBe true
      MonthValidator.isValid("DEC") shouldBe true
    }

    "return true for valid lowercase month abbreviations" in {
      MonthValidator.isValid("jan") shouldBe true
      MonthValidator.isValid("feb") shouldBe true
      MonthValidator.isValid("dec") shouldBe true
    }

    "return false for invalid month strings" in {
      MonthValidator.isValid("January") shouldBe false
      MonthValidator.isValid("abc")     shouldBe false
      MonthValidator.isValid("")        shouldBe false
      MonthValidator.isValid("JUNEE")   shouldBe false
    }

    "return false for null input" in {
      MonthValidator.isValid(null) shouldBe false
    }
  }
}
