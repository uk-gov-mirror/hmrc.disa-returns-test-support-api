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

import org.apache.pekko.stream.Materializer
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.test.DefaultAwaitTimeout
import uk.gov.hmrc.disareturnstestsupportapi.config.AppConfig
import uk.gov.hmrc.http.HeaderCarrier
import utils.WiremockHelper._

import scala.concurrent.ExecutionContext

trait BaseIntegrationSpec
    extends AnyWordSpec
    with Matchers
    with GuiceOneServerPerSuite
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with DefaultAwaitTimeout
    with WiremockHelper
    with CommonStubs {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure(config)
    .build()

  def config: Map[String, String] =
    Map(
      "auditing.enabled"                              -> "false",
      "microservice.services.auth.host"               -> wiremockHost,
      "microservice.services.auth.port"               -> wiremockPort.toString,
      "microservice.services.disa-returns-stubs.host" -> wiremockHost,
      "microservice.services.disa-returns-stubs.port" -> wiremockPort.toString,
      "microservice.services.disa-returns.host"       -> wiremockHost,
      "microservice.services.disa-returns.port"       -> wiremockPort.toString
    )

  override def beforeAll(): Unit = {
    startWiremock()
    super.beforeAll()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  override def beforeEach(): Unit = {
    resetWiremock()
    super.beforeEach()
  }

  implicit val mat:              Materializer     = app.injector.instanceOf[Materializer]
  implicit val ws:               WSClient         = app.injector.instanceOf[WSClient]
  implicit val executionContext: ExecutionContext = app.injector.instanceOf[ExecutionContext]
  implicit val appConfig:        AppConfig        = app.injector.instanceOf[AppConfig]
}
