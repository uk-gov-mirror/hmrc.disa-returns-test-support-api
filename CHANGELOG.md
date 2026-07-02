# DISA Returns Test Support API Changelog

All notable changes to this API will be documented in this file. Learn about API features, fixes, deprecations and documentation changes.

## 1 July 2026

### Updates

- The generate reconciliation report endpoint now validates request bodies using a strict JSON parser. Empty or whitespace-only payloads return the existing `EMPTY_PAYLOAD` error response.
- Added new error response `MALFORMED_JSON` (400) returned when the request body contains invalid JSON.
- Updated request body validation failures to return `VALIDATION_FAILURE` (400) with field-specific errors. Each field error includes a `code`, `message` and JSON pointer `path`.
- Added new field-level validation error codes `MISSING_FIELD` and `VALIDATION_ERROR`.
- Updated the OpenAPI schema to document field-specific validation errors for the generate reconciliation report endpoint.

### What impact does this have?

- Consumers must send a valid JSON request body to the generate reconciliation report endpoint.
- Consumers should handle the new `MALFORMED_JSON`, `VALIDATION_FAILURE`, `MISSING_FIELD` and `VALIDATION_ERROR` error codes.
- Consumers currently parsing validation errors from the previous `issues` response field must update their error handling to use the new `errors` array.
