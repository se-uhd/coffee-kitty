# Do the read methods of the data adapters run in a transaction?

## Decision Metadata

### Status

Accepted

### Date

2026-11-24

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

`application.yaml` turns off Open Session in View, so a lazy association of a JPA entity loads only inside a
transaction. A data adapter reads a JPA entity and maps it to a domain object. Today, the read methods of the data
adapters, except `getByUserIdForUpdate`, have no `@Transactional`, and every association is eager. This ADR records
the decision and names the condition for a change.

## Considered Options

* Eager associations and read methods without a transaction. A read method works for a caller with and without a
  transaction. Hibernate loads the associations of every row, even when the caller does not need them, and runs one
  query per referenced row for a list (N+1 queries).
* Read methods with `@Transactional(readOnly = true)` and lazy associations. The data adapter maps the JPA entity inside
  the transaction, and a query can fetch only what its caller needs. Every read method needs the annotation, and a read
  method without it fails only at runtime with a `LazyInitializationException`.
* Open Session in View. A lazy association loads until the server has written the response. The request then holds a
  database connection for its whole duration, and a query can run while Jackson serializes the response.

## Decision(s)

The read methods have no transaction, and the associations are eager (the first option). A coffee group has few users,
and the lists of the API need few queries: `GET /api/users` loads no association, and the corrections of one user refer
to that user and to the few admins who corrected the coffee count. The rule for an exception is in `CLAUDE.md`: A data
adapter for a JPA entity with a lazy association reads and maps the entity inside a `@Transactional` method.

## Consequences

* A new association is eager by default (`@ManyToOne`), or a developer has to declare it eager (`@ElementCollection`).
* `getAll` of an entity with associations runs N+1 queries. No endpoint calls such a function for a long list today.
* Revisit this decision when an endpoint lists many rows with associations, such as the payments of all users. The
  second option then replaces this one in a new ADR.
