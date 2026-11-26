# Which media type does an error response have?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The REST API reads and writes only JSON. Since 2026-11-22, `ApiWebConfig` removes the XML and YAML message converters,
so a request with the header `Accept: application/yaml` gets 406 Not Acceptable. Spring MVC chooses the media type of a
response body by content negotiation with the `Accept` header, unless the response already has a content type
([`AbstractMessageConverterMethodProcessor`][preset]). Since the first version of 2026-10-31,
`GlobalExceptionHandler.respond` sets the content type of each error response that the exception handler writes. The
KDoc of `respond` gave the reason until the cleanup of the comments on 2026-11-26 (ADR 013) deleted it, because a
private function has no KDoc.

## Considered Options

* Content negotiation for an error response, as for any other response. The media type of the error body then depends
  on the `Accept` header, and a client whose `Accept` header lists no JSON does not get the JSON body.
* The content type `application/json;charset=UTF-8` on every error response, which skips content negotiation.

## Decision(s)

Every error response that `GlobalExceptionHandler` writes has the content type `application/json;charset=UTF-8` (the
second option). The client gets the JSON `ErrorResponse` for any `Accept` header, also with the 406 for an `Accept`
header without JSON. The charset is the one that `ApiWebConfig` adds to the JSON responses.

## Consequences

* `ErrorResponseSystemTests` checks the content type of an error response and the error code in the body of the 406
  for `Accept: application/yaml`.
* The code that writes an error response before Spring MVC, such as `JsonAuthenticationEntryPoint`, sets the same
  content type in `writeErrorResponse`.

[preset]: https://github.com/spring-projects/spring-framework/blob/main/spring-webmvc/src/main/java/org/springframework/web/servlet/mvc/method/annotation/AbstractMessageConverterMethodProcessor.java
