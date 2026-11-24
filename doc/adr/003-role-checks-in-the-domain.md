# Which module checks the role of the acting user?

## Decision Metadata

### Status

Proposed

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision pending)
* Claude Code (analysis)

## Context

Only an admin may create a user, correct a coffee count, or list the corrections. Coffee Kitty has no authentication
yet: The header `X-Acting-User` contains the login name of the acting user. The check of the role can be a rule of
Spring Security in the api module or a check in the domain. Today, the domain checks the role with `requireAdmin`. No
ADR records this decision. This ADR proposes to keep it.

## Considered Options

* Rules of Spring Security in the api module, such as `hasRole("ADMIN")` for a route or `@PreAuthorize` on a controller
  function. The domain has no code for authorization. A caller of an API port that is not a controller, such as a
  future message consumer, bypasses the check.
* A check in the domain. Each API port function that needs a role takes the acting user as a parameter and calls
  `requireAdmin`, which throws a `ForbiddenException`. The api module only identifies the acting user.
* Both. The rule exists twice and can differ.

## Decision(s)

The domain checks the role (the second option). Who may correct a coffee count is a business rule of FR-8, so it
belongs to the domain and must not depend on how the api module identifies the acting user. The api module has two
tasks: `ActingUserAuthenticationFilter` resolves the acting user, and `SecurityConfig` requires an acting user for the
routes of the controller functions with `ActingUserRequired`, so that a request without one gets its 401 before Spring
MVC validates the body.

## Consequences

* A unit test of a domain service checks the role without Spring Security.
* The principal of a request has no authorities, and no rule of Spring Security reads a role.
* A caller without an acting user, such as the bootstrap admin loader, needs a function of an internal port, which the
  api module must not use (`UserInternalService`).
* When Coffee Kitty gets authentication, only the filter changes. The domain keeps its checks.
