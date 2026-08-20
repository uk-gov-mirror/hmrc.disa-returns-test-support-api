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

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.{Materializer, SystemMaterializer}
import org.mockito.Mockito
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.disareturnstestsupportapi.connectors.{DisaReturnsCallbackConnector, GenerateReportConnector, ReportingWindowOverrideConnector}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.ExecutionContext

abstract class BaseUnitSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with EitherValues
    with ScalaFutures
    with MockitoSugar
    with DefaultAwaitTimeout
    with GuiceOneAppPerSuite
    with MockAuthConnector {

  implicit val ec:     ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc:     HeaderCarrier    = HeaderCarrier()
  implicit val system: ActorSystem      = ActorSystem("TestSystem")
  implicit val mat:    Materializer     = SystemMaterializer(system).materializer

  override def beforeEach(): Unit =
    Mockito.reset(
      mockAuthConnector,
      mockHttpClient,
      mockAppConfig,
      mockRequestBuilder,
      mockGenerateReportConnector,
      mockDisaReturnsCallbackConnector,
      mockReportingWindowOverrideConnector
    )

  val mockHttpClient:                       HttpClientV2                     = mock[HttpClientV2]
  val mockAppConfig:                        AppConfig                        = mock[AppConfig]
  val mockRequestBuilder:                   RequestBuilder                   = mock[RequestBuilder]
  val mockGenerateReportConnector:          GenerateReportConnector          = mock[GenerateReportConnector]
  val mockDisaReturnsCallbackConnector:     DisaReturnsCallbackConnector     = mock[DisaReturnsCallbackConnector]
  val mockReportingWindowOverrideConnector: ReportingWindowOverrideConnector = mock[ReportingWindowOverrideConnector]

  override def fakeApplication(): Application = GuiceApplicationBuilder()
    .overrides(
      bind[AuthConnector].toInstance(mockAuthConnector),
      bind[AppConfig].toInstance(mockAppConfig),
      bind[DisaReturnsCallbackConnector].toInstance(mockDisaReturnsCallbackConnector),
      bind[GenerateReportConnector].toInstance(mockGenerateReportConnector),
      bind[ReportingWindowOverrideConnector].toInstance(mockReportingWindowOverrideConnector)
    )
    .build()
}
