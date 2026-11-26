# When does the API refuse an `Accept` header that it cannot serve?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis and implementation)

## Context

Spring MVC compares the `produces` of a controller function with the `Accept` header of a request when it looks up the
function, before it reads the request. A function without `produces` gets the media type of its response only when
Spring writes the return value, after the function ran. No controller function of Coffee Kitty declared `produces`. A
`POST /api/users/{userId}/consumption` with `Accept: text/plain` therefore added a coffee and then answered 406 Not
Acceptable, so the client saw an error for a coffee that counted. The same held for creating a user, undoing a coffee,
and correcting a coffee count. The coffee app of SE@UHD found the same problem and decided it in its ADR 070.

## Considered Options

* Keep the negotiation after the controller function, and document that a 406 can follow a change.
* Check the `Accept` header in a filter before Spring MVC.
* Declare `produces` on every controller.

## Decision(s)

Every controller declares `produces = [MediaType.APPLICATION_JSON_VALUE]` on its `@RequestMapping` (the third option).
Spring then refuses a request whose `Accept` header excludes JSON in the lookup of the function, before it binds the
arguments and runs the function, and the response is the 406 of ADR 019 with the JSON body of ADR 022. A filter would
repeat the negotiation of Spring and would have to know the media types of each function.

`ApiWebConfig` gives the JSON converters the default charset UTF-8 instead of a charset on their media types, because
Spring takes the media type of a response from `produces`, which has no charset, and adds the default charset of the
converter.

## Consequences

* A request whose `Accept` header excludes JSON changes nothing and gets 406. `ErrorResponseSystemTests` checks it for
  each operation that changes data.
* The responses keep the content type `application/json;charset=UTF-8`.
* A new controller needs `produces` on its `@RequestMapping` (`.claude/rules/api.md`). No check finds a controller
  without it.
