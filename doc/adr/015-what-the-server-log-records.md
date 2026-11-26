# What does the server log record about a request?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (analysis)

## Context

The server log receives text that a client sends, such as a path, a field of a request body, or a message of Jackson
about the body. ADR 013 removed the comments in `DomainException` and `GlobalExceptionHandler` that stated how the log
treats this text.

## Considered Options

* Log the text of a request as it arrives.
* Log a user only by the id, and write each text of a client as one shortened line.

## Decision(s)

The log treats the text of a client as untrusted input (the second option):

* A log line identifies a user only by the id, never by a login name, an email address, or another value that the
  user entered. `DomainException.logMessage` is the message of a domain exception without such values.
* `GlobalExceptionHandler.loggableText` replaces each control character, such as CR and LF, and each Unicode line or
  paragraph separator with a space, so that a client cannot add a forged line to the log.
* `loggableText` cuts a text to 500 characters, so that a client cannot fill the log with a long value. ADR-010
  records why 500 characters leave the text of every legitimate request whole.

## Consequences

* A new log line about a request writes the text of the client through `loggableText` and a user by the id.
* The tests of the log messages check which values a message leaves out, such as a login name.
