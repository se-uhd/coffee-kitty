# Which endpoints need an acting user?

## Decision Metadata

### Status

Accepted

### Date

2026-11-25

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

`SecurityConfig` required an acting user only for the endpoint of a controller function with the annotation
`ActingUserRequired` ([ADR-003](003-role-checks-in-the-domain.md) names this annotation). Every other endpoint was open
to every client. A developer who added an endpoint and forgot the annotation got an open endpoint, and the build did
not detect it: The architecture test only checked the functions that call `CurrentUserProvider.currentUser`, and a
function that only reads data does not call it. FR-15 and FR-16 require a 401 for a request without a known acting user
that reads a balance. An implementation of these two endpoints without the annotation passed every check and answered
such a request with 200.

## Considered Options

* Keep the annotation `ActingUserRequired`, and review every new endpoint.
* Require one of two annotations on every controller function, and fail an architecture test without one.
* Require an acting user for every controller function, and mark the open endpoints with an annotation.

## Decision(s)

Every controller function of Coffee Kitty needs an acting user, unless it has the annotation `OpenEndpoint` (the third
option). A forgotten annotation then gives a 401, which a developer notices at the first request, and never an open
endpoint. The annotation `ActingUserRequired` no longer exists. The second option also detects a forgotten annotation,
but it needs two annotations and a further architecture test for the same result.

`SecurityConfig` reads the controller functions from Spring MVC, as before. It requires authentication for the HTTP
method and the path of each function without `OpenEndpoint`, and also for `HEAD` on the path of a `GET` endpoint,
because Spring MVC answers a `HEAD` request with the controller function of the `GET` endpoint. A path that no
controller of Coffee Kitty serves stays open, so that an unknown path gets a 404 and the dev profile serves Swagger UI.

## Consequences

* An endpoint is open only where its controller function says so. A reviewer finds every open endpoint by searching for
  `OpenEndpoint`.
* A new endpoint needs no annotation to require an acting user. The sentence on `ActingUserRequired` in ADR-003 is
  outdated, and its decision on the role checks stays.
* An architecture test fails for a function with `OpenEndpoint` that calls `CurrentUserProvider.currentUser`, because
  an open endpoint has no acting user.
* The six endpoints that were open stay open: reading the users, a user, a coffee count, and the undo status, adding a
  coffee, and undoing a coffee. FR-3 and FR-13 leave open who may send these requests until Coffee Kitty has
  authentication.
