
# disa-returns-test-support-api

This service provides a **testing scaffold** for developers and ISA Managers integrating with the [DISA Returns API](https://github.com/hmrc/disa-returns).

It enables simulation of return submission and reconciliation scenarios that ISA Managers will encounter in production, allowing validation of integrations and error-handling logic before go-live.

It is available in development environments and external test. External test will be the sandbox integration environment for API consumers building applications.

## Available endpoints

### Generate reconciliation report

#### Endpoint summary

This endpoint enables simulation of the scenario where a user has successfully submitted and declared their return, and **NPS** makes a callback to the **DISA Returns API** to indicate the reconciliation report is ready for retrieval.

Note that in the case there are no reconciliation issues, NPS will not generate a report or make any callback. This scenario is not currently supported and this endpoint will generate a report for all valid inputs.

Once called, this procedure will make calls to both `disa-returns` NPS callback route to provide a return summary, and also to `disa-returns-stub`, which will hold the generated reconciliation report.

The link provided in the return summary via the callback can then be utilised to retrieve the report on the stub, as it would be from NPS.

| Path | Method | Auth          | Purpose                                                               |
|---|---|---------------|-----------------------------------------------------------------------|
| `/monthly/:zRef/reconciliation` | **POST** | *X-Client-ID* | Simulate NPS reconciliation report generation for a given ZREF.       |
| `/monthly/:zRef/reporting-window-override` | **PUT** | OAuth bearer token | Temporarily override the reporting window for the authenticated user. |


#### Path parameters

| Name | Type |   Example | Description | Constraints        |
|---|---|----------:|---|--------------------|
| `zRef` | `string` |   `Z1234` | ISA Manager reference for the return. | `^[z\|Z][0-9]{4}$` |

#### Request body

The body should be a JSON representation of the following case class:

```scala
case class GenerateReportRequest(
  oversubscribed:    Int,
  traceAndMatch:     Int,
  failedEligibility: Int
)
```

### Override the reporting window

This endpoint enables an authenticated user to test the monthly returns journey outside a live reporting period.

The override is associated with the credential ID obtained from the caller's authenticated session. It only affects that user and replaces any override they previously configured.

```http
PUT /monthly/Z1234/reporting-window-override
Content-Type: application/json
Authorization: Bearer <access-token>
Accept: application/vnd.hmrc.1.0+json
```

```json
{
  "startDate": "2026-08-13T00:00:00Z",
  "endDate": "2026-08-31T23:59:59Z"
}
```

Both fields are required RFC3339 date-time values with a timezone. `startDate` must be before or equal to `endDate`, and both boundaries are inclusive.

A successful request returns `204 No Content`. The override lasts for one hour from the most recent successful request. The system clock is not changed or mocked. When the override expires, normal reporting-period validation resumes automatically.

## Running the app

```bash
# Run the app locally with service manager
sm2 --start DISA_RETURNS_ALL
```

## Running the test suite

To run the unit tests:

```bash
sbt test
```

To run the integration tests:

```bash
sbt it/test
```

## Before you commit

This service leverages scalaFmt to ensure that the code is formatted correctly.

Before you commit, please run the following commands to check that the code is formatted correctly:

```bash
# checks all source files are correctly formatted
sbt scalafmtCheckAll

# checks all sbt files are correctly formatted
sbt scalafmtSbtCheck

# if checks fail, you can format with the following commands

# formats all source files
sbt scalafmtAll

# formats all sbt files
sbt scalafmtSbt

# formats just the main source files (excludes test and configuration files)
sbt scalafmt
```

## Viewing the API specifications

*For internal HMRC developers.*

This repository contains API definitions for the DSA Returns API, deployed to the API platform.

To view and test this documentation locally, follow the instructions below.

```zsh
# Run the API platform devhub preview locally with service manager
sm2 -start DEVHUB_PREVIEW_OPENAPI

# Run disa returns locally
sbt run

# Open the API platform devhub preview in your browser
open http://localhost:9680/api-documentation/docs/openapi/preview/
```

From this page, you can enter the fully qualified url of the documentation you wish to view, for example:

```
http://localhost:1206/api/conf/1.0/application.yaml
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").
