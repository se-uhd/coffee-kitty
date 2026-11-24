# Do the read methods of the data adapters run in a transaction?

## Decision Metadata

### Status

Proposed

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision pending)
* Claude Code (analysis)

## Context

`application.yaml` turns off Open Session in View, so a lazy association of a JPA entity loads only inside a
transaction. A data adapter reads a JPA entity and maps it to a domain object. Today, the read methods of the data
adapters, except `getByUserIdForUpdate`, have no `@Transactional`, and every association is eager. No ADR records this
decision. This ADR proposes to keep it for now and names the condition for a change.

## Considered Options

* Read methods without a transaction and eager associations. A read method works for a caller with and without a
  transaction. Hibernate loads the associations of every row, also when the caller does not need them, and runs one
  query per referenced row for a list (N+1 queries).
* Read methods with `@Transactional(readOnly = true)` and lazy associations. The adapter maps the entity inside the
  transaction, and a query can fetch only what its caller needs. Every read method needs the annotation, and a missing
  one fails only at runtime with a `LazyInitializationException`.
* Open Session in View. A lazy association loads until the response is written. The request then holds a database
  connection for its whole duration, and a query can run while Jackson serializes the response.

## Decision(s)

The read methods have no transaction, and the associations are eager (the first option). A coffee group has few users,
and every list of the API is short: `GET /api/users` loads no association, and the corrections of one user share two
referenced users. The rule for an exception is in `CLAUDE.md`: A data adapter for a JPA entity with a lazy association
reads and maps the entity inside a `@Transactional` method.

## Consequences

* A new association is eager by default (`@ManyToOne`) or has to be declared eager (`@ElementCollection`).
* `getAll` of an entity with associations runs N+1 queries. No endpoint calls such a function for a long list today.
* Revisit this decision when an endpoint lists many rows with associations, such as the payments of all users. The
  second option then replaces this one in a new ADR.
