---
paths:
  - "models/**"
  - "doc/statecharts/**"
  - "doc/class-diagrams/**"
---

# Models

`models/README.md` lists the TLA+ and Alloy models with their tests and diagrams. `.claude/skills/statechart/SKILL.md`
describes how to write a TLA+ model, its statechart, and its replay test. ADR-012 records why the build checks the
models against the code.

- The subproject `models` is not a module of the app. Its tests depend on `domain`, and no module depends on it.
- Commit each diagram together with its model. `gradle :models:check` fails when a diagram differs from its model.
- The model tests may check an implementation detail that a model assumes, such as an annotation or the function of the
  data port that a service calls, because the verdict of the model depends on it. Other tests never check such details.
- `.claude/rules/domain.md` lists the changes to `DomainModel.als` and `DomainModelConformanceTest` that a new or
  changed property of a domain model needs.
- Until 2026-12-08, `Consumption.tla` leaves out the undo status (FR-9). `CLAUDE.md` states what changes on that date.
