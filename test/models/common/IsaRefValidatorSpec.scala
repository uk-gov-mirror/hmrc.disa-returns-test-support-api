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

import uk.gov.hmrc.disareturnstestsupportapi.models.validators.IsaRefValidator
import utils.BaseUnitSpec

class IsaRefValidatorSpec extends BaseUnitSpec {

  "IsaRefValidator.isValid" should {

    "return true for valid ZRef format" in {
      IsaRefValidator.isValid("Z1234") shouldBe true
      IsaRefValidator.isValid("Z0000") shouldBe true
      IsaRefValidator.isValid("Z9999") shouldBe true
      IsaRefValidator.isValid("z1234") shouldBe true
    }

    "return false for missing Z prefix" in {
      IsaRefValidator.isValid("1234")  shouldBe false
      IsaRefValidator.isValid("A1234") shouldBe false
    }

    "return false for incorrect length" in {
      IsaRefValidator.isValid("Z123")   shouldBe false
      IsaRefValidator.isValid("Z12345") shouldBe false
    }

    "return false for non-numeric suffix" in {
      IsaRefValidator.isValid("Z12A4") shouldBe false
      IsaRefValidator.isValid("Zabcd") shouldBe false
    }

    "return false for empty" in {
      IsaRefValidator.isValid("") shouldBe false
    }
  }
}
