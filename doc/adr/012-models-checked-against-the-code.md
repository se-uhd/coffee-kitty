# How does the build check formal models against the code?

## Decision Metadata

### Status

Accepted

### Date

2026-11-26

### Involved

* Sebastian Baltes (decision)
* Claude Code (implementation and analysis)

## Context

Agents write much of the code of Coffee Kitty. A reviewer reads the code of one function at a time, but a flow such as
the coffee count, with adding, undoing, and correcting, depends on the order of the steps. The code does not show which
states a user can reach and which steps lead between them. A statechart shows them, and a model checker can derive the
chart from a formal model and check rules in every state. A model or a diagram that nobody checks against the code
becomes wrong when the code changes, as an outdated comment does.

## Considered Options

* Draw the diagrams by hand in `doc/`.
* Write TLA+ and Alloy models and check only their own rules with TLC and the Alloy Analyzer.
* Write the models, check their rules, draw the diagrams from them, and test the models against the code in
  `gradle check`.

## Decision(s)

The build checks the models against the code (the third option). The subproject `models` holds `Consumption.tla`, a
model of the coffee count of a user, `ConsumptionConcurrency.tla`, a model of concurrent requests, and
`DomainModel.als`, a model of the classes in `domain/.../model/` and the rules for their values. The models are
optional for students, who meet TLA+ and Alloy only in a demo of the lecture. Gradle therefore includes the subproject
only with `-Pmodels`, and CI includes it only in se-uhd/coffee-kitty. `gradle -Pmodels check` fails in these cases:

* A rule of a TLA+ model or an assertion of the Alloy model breaks.
* A diagram in `doc/statecharts/` or `doc/class-diagrams/` differs from its model.
* `ConsumptionConformanceTest` replays a step of `Consumption.tla` that `CoffeeConsumptionServiceImpl` does not take.
* `DomainModelConformanceTest` finds a class, a property, or a rule for a value that differs from `DomainModel.als`.

## Consequences

* mise installs TLC (`tla2tools.jar`) from the URL of a release with a checksum. `mise outdated` cannot report a newer
  release of TLC, so a developer updates its version in `mise.toml` by hand.
* The build needs no Graphviz, because PlantUML lays out the diagrams with Smetana. PlantUML measures text with the
  fonts of the platform, so the check compares the PlantUML source that each SVG embeds, not the bytes of the SVG.
* The models have bounds, such as the largest count `MaxCount` and a scope of 4 objects in Alloy. A fault that needs
  larger values can stay hidden. The replay test therefore builds each state of the model a second time with counts up
  to `Int.MAX_VALUE`.
* A change to `CoffeeConsumptionServiceImpl` or to a class in `domain/.../model/` needs a change to its model before it
  goes to `main` of se-uhd/coffee-kitty (`.claude/rules/domain.md`). A fork, such as the fork of a student, does not.
* The model leaves out the undo status (FR-9) until 2026-12-08, because the tests of the undo status are the reference
  solution of assignment 7.
