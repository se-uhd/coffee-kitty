# models

This subproject holds formal models of Coffee Kitty, checks them against the code, and draws them as diagrams.

| Model | Tool and tests | Diagram |
|---|---|---|
| `src/main/tla/Consumption.tla` | TLC, `ConsumptionConformanceTest` | `doc/statecharts/Consumption.svg` |
| `src/main/tla/ConsumptionConcurrency.tla` | TLC, `ConsumptionConcurrencyTest` | none |
| `src/main/alloy/DomainModel.als` | Alloy Analyzer, `DomainModelConformanceTest` | `doc/class-diagrams/DomainModel.svg` |

```shell
gradle -Pmodels :models:statecharts      # checks the TLA+ models with TLC and writes the statechart
gradle -Pmodels :models:classDiagrams    # writes the class diagram of each Alloy model
gradle -Pmodels :models:check            # runs the tests and fails when a rule breaks or a diagram differs from its model
```

Gradle includes this subproject only with `-Pmodels`, or with `models=true` in `~/.gradle/gradle.properties`, which also
shows it in IntelliJ IDEA. Without it, `gradle build` leaves out the models, so that a student needs no TLA+ or Alloy.

Commit each diagram together with its model. `.claude/skills/statechart/SKILL.md` describes the TLA+ models and the
statechart.

## Class diagram

`ClassDiagram.kt` draws each class sig of an Alloy model as a class and each enum sig as an enum. A field with a value
sig, such as `Text`, or with an enum is an attribute. A field with a class sig is an arrow. The head of an arrow has the
multiplicity of the field. The tail has the number of objects that can refer to one object. The Alloy Analyzer finds
this number in two runs within a scope of 4, so the number follows the facts of the model. For example, because of the
fact `OneConsumptionPerUser`, the tail of the arrow from `CoffeeConsumption` to `User` has the multiplicity 1. A legend
lists the facts and whether each assertion holds.

The diagram leaves out the value rules, that is, the predicates `valid<Sig><Field>`. The model states these rules with
value classes such as `TextWithNul`, and the requirements state them in words.

## Possible extension: object diagrams

The class diagram shows that an assertion breaks, but not the objects that break it. An object diagram of the
counterexample could show them, in the same way as the report of a statechart shows the shortest path to a broken rule.
Such a diagram would belong in the build directory next to the report, not in `doc/`. An object diagram of an instance
of each run command could also show an example of valid objects.

`NoTextPropertyAcceptsNul` concerns values, so its counterexample would contain no objects worth drawing.
`AsManyConsumptionsAsUsers` concerns objects, but it holds because of the fact `OneConsumptionPerUser`. Object diagrams
become useful when an assertion about the relations between objects breaks, such as a rule about the beans that a bean
is merged into. Until then, the Alloy Analyzer in the Alloy jar of the build shows the instances of a model in its
visualizer.
