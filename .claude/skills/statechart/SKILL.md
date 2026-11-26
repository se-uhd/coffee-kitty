---
name: statechart
description: "Model a flow of Coffee Kitty in TLA+, check its rules with TLC, and draw it as a PlantUML statechart for a stakeholder to review. Use for /statechart, to review a requirement as states and steps, or to show a bug as a path through states."
---

# Statechart

A statechart shows one flow of Coffee Kitty as states and steps, labeled in the words of the person who judges the
behavior: the stakeholder of a requirement, a student, or a reviewer. TLC checks the rules of the flow in every state of
a TLA+ model, and the chart marks the states in which a rule breaks.

## Files

| File | Content |
|---|---|
| `models/src/main/tla/<Model>.tla` | the TLA+ module of the model |
| `models/src/main/tla/<Model>.cfg` | the constants and the rules (INVARIANTS) of the model |
| `models/src/main/kotlin/de/seuhd/campuscoffee/models/statechart/views/<Model>View.kt` | the words of the chart |
| `doc/statecharts/<Model>.puml` and `<Model>.svg` | the chart |
| `models/build/statecharts/<Model>/report.txt` | the report, next to the output of each run of TLC |

`VIEWS` in `Statechart.kt` lists the view of each model that has a chart. The model `Consumption` covers the coffee
count of a user (FR-3, FR-8, FR-13).

## Run

```shell
mise install                          # once: installs tla2tools.jar, which mise.toml pins with its checksum
gradle :models:statecharts        # checks each model and writes the chart of each model with a view
gradle :models:checkStatecharts   # part of `gradle check`
```

`checkStatecharts` fails when a rule breaks or when a chart differs from its model. Commit the `.puml` and the `.svg`
together with the model. PlantUML lays out the chart with Smetana, its port of Graphviz dot, so the build needs no
Graphviz.

## Write the model

- One model covers one flow. The comment at the top of the module names the requirements and the classes that it
  follows.
- For a model of existing code, give each check of the code a named operator and name the function in a comment, such as
  `CanUndo` for `CoffeeConsumptionServiceImpl.undo`.
- Each rule that the stakeholder accepts is an INVARIANT in the `.cfg`. A rule about information that the model does
  not hold needs a new variable. A broken rule is a finding, so do not change the rule to make it hold.
- `Next` is a disjunction of named actions, such as `AddCoffee \/ Undo`, because the view labels each step by its
  action.
- Small constants, such as `MaxCount = 2`, and an order of events instead of a clock keep the number of states small.
  `Consumption` stores each coffee time as "recent" or "old" and ends the grace periods in the order of the coffees.
- The command reports a state without a next step as the broken rule `NoDeadlock`. When such a state is a correct end,
  add an action that only this state enables and that changes no variable.

## Write the view

- `state()` maps a model state to its label, after the labels of its parent states, such as
  `listOf("The coffee count is above 0", "A user can undo a coffee")`. A label states the property that defines the
  state, with the terms of `GLOSSARY.md`, not the names of variables. Model states with the same label become one chart
  state.
- `step()` maps a step to the label of its arrow, or returns null to hide the step. A label names the actor in the third
  person, such as "a user adds a coffee" or "an admin corrects the coffee count of a user". The reader of the chart is a
  stakeholder or a reviewer, not a user in the model, so a label never says "you". A label says "a user", not "the
  user", because no label refers to a user that the reader already knows.
- `rules` maps each INVARIANT to the sentence that the stakeholder accepted.
- `panels` splits the chart into panels side by side, such as one for the steps of a user and one for the steps of an
  admin. Each panel draws every state and the arrows of the actions that it lists. The command fails unless exactly one
  panel lists the action of an arrow. Use panels when the steps of one actor lead between almost all states and hide the
  flow of another actor.
- A chart with more than about 20 arrows is hard to review. Merge states that behave the same for the stakeholder, and
  keep two states apart when they break different rules.
- A step that stays in one chart state has no arrow. The report lists such steps, and a finer label makes one visible.

## Read the report

- TLC names only the first rule that a state breaks, so the command checks each rule in a run of its own, after a
  run that writes the complete state graph. A state of the graph without a step out breaks the rule `NoDeadlock`.
  The report gives the shortest path to a state that breaks each rule, in the words of the chart.
- Red marks a chart state in which every model state breaks a rule, and orange one in which only some do. The legend
  names the rules.
- The run that writes the state graph checks no rule, so the chart also contains the states after a state that breaks
  a rule.

## Link the model to the code

A model of existing code gets a replay test in `models/src/test/kotlin/`, such as `ConsumptionConformanceTest`:

- The test runs TLC with a larger bound than the chart (`ConsumptionReplay.cfg`). For each step, it builds the state
  before the step in the service with several sets of coffee times, including the last instant of a grace period, calls
  the function of the action, and compares the result with the state after the step.
- Random walks start at the initial state, let the service build its own state, and move the clock between the steps.
- `ConsumptionMapping` names the function of each action and the functions that change nothing. Two tests fail when an
  action of the model or a function of the port has no counterpart.
- An input class of the model becomes a concrete value at its limit, such as a note of `MAX_NOTE_LENGTH` characters for
  the class "longest" and one of `MAX_NOTE_LENGTH + 1` characters for the class "too long".
- A bound of the model that stands for a limit of the code is replayed at that limit. `MaxCount` stands for the largest
  count that the service stores, so the test builds each state a second time with each count raised by
  `Int.MAX_VALUE - MaxCount`.

A model of concurrent requests, such as `ConsumptionConcurrency.tla`, has no chart. It extends the model of one request
and computes the result of each request with its operators, which the replay test of `Consumption.tla` checks against
the service. Its rule `Serializable` requires that the requests end in a state that some order of the same requests
reaches one after another. `ConsumptionConcurrencyTest` checks the model with and without the row lock and checks that
each function that changes a consumption runs in a transaction. `ConsumptionConformanceTest` checks the other assumption
of the model, that each such function reads the consumption with `getByUserIdForUpdate`.

The models leave out more stored coffee times than the bound allows and time that passes while concurrent requests
run.

## Hand off

Give the stakeholder the SVG, and for each broken rule its sentence and its shortest path from the report. Name what the
model leaves out. When the replay test fails, compare the step with the requirement before you change the model or the
code.
