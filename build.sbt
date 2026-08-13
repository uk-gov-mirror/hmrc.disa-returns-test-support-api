import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.6"
ThisBuild / scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"

lazy val microservice = Project("disa-returns-test-support-api", file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    scalacOptions += "-Wconf:src=routes/.*:s"
  )
  .settings(CodeCoverageSettings.settings*)
  .settings(
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources"
  )
  .settings(PlayKeys.playDefaultPort := 1206)

Test / javaOptions += "-Dlogger.resource=logback-test.xml"
Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
Test / fork := true

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.it)

addCommandAlias("prePrChecks", ";scalafmtCheckAll;scalafmtSbtCheck")
addCommandAlias("precommit", ";scalafmtAll;test:scalafmtAll;it/test:scalafmtAll;coverage;test;it/test;coverageReport")
