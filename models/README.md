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

`ClassDiagram.kt` draws each class sig of an Alloy model as a class and each enum sig as an enum. A field of a value
sig, such as `Text`, or of an enum is an attribute, and a field of a class sig is an association. The multiplicity at
the target end follows the field declaration: `one` gives 1, `lone` 0..1, `some` 1..*, and `set` *. An Alloy field
declares no multiplicity for the source end, so two runs of the Alloy Analyzer with a scope of 4 derive it. One run
checks whether a target object can exist without a source object, and the other whether two source objects can refer to
the same target object. For example, the fact `OneConsumptionPerUser` gives 1 at the `CoffeeConsumption` end of the
association from `CoffeeConsumption` to `User`. The legend lists the facts and whether each assertion holds. The title
is the start of the header comment of the model, such as "Domain model of Coffee Kitty", and the file name.

The diagram leaves out the value rules, that is, the predicates `valid<Sig><Field>`. The model states these rules with
value classes such as `TextWithNul`, and the requirements state them in words.

## Possible extension: object diagrams

The class diagram shows that an assertion breaks, but not the objects that break it. An object diagram of the
counterexample could show them, in the same way as the report of a statechart shows the shortest path to a broken rule.
Such a diagram would belong in the build directory next to the report, not in `doc/`. An object diagram of an instance
of each run command could also show an example of valid objects.

`NoTextPropertyAcceptsNUL` concerns values, so its counterexample would contain no objects worth drawing.
`AsManyConsumptionsAsUsers` concerns objects, but it holds because of the fact `OneConsumptionPerUser`. Object diagrams
become useful when an assertion about the relations between objects breaks, such as a rule about the beans that a bean
is merged into. Until then, the Alloy Analyzer in the Alloy jar of the build shows the instances of a model in its
visualizer.
