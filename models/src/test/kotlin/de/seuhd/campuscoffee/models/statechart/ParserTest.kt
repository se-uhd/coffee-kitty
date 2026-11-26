package de.seuhd.campuscoffee.models.statechart

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ParserTest {
    @Test
    fun `parseTlaValue reads nested sequences, records, sets, and functions`() {
        val value = parseTlaValue("""<<[paid |-> TRUE, note |-> "a \"b\""], {1, -2}, (alice :> 3 @@ bob :> 0)>>""")

        assertThat(value).isEqualTo(
            TlaValue.SeqValue(
                listOf(
                    TlaValue.RecordValue(
                        mapOf("paid" to TlaValue.BoolValue(true), "note" to TlaValue.StringValue("a \"b\""))
                    ),
                    TlaValue.SetValue(listOf(TlaValue.IntValue(1), TlaValue.IntValue(-2))),
                    TlaValue.FunctionValue(
                        listOf(
                            TlaValue.NameValue("alice") to TlaValue.IntValue(3),
                            TlaValue.NameValue("bob") to TlaValue.IntValue(0)
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `parseTlaValue keeps text that it cannot read as a name`() {
        assertThat(parseTlaValue("Nat \\ {0}")).isEqualTo(TlaValue.NameValue("Nat \\ {0}"))
    }

    @Test
    fun `parseModelState joins a value that continues on the next line`() {
        val state = parseModelState("/\\ times = <<\"recent\",\n   \"old\">>\n/\\ count = 2")

        assertThat(state["times"].items.map { it.string }).containsExactly("recent", "old")
    }

    @Test
    fun `parseDot reads the states, the initial state, and the steps of a TLC dump`() {
        val dot =
            """
            strict digraph DiskGraph {
            1 [label="/\\ times = <<>>\n/\\ count = 0",style = filled]
            1 -> -2 [label="AddCoffee",color="black",fontcolor="black"];
            -2 [label="/\\ times = <<\"recent\">>\n/\\ count = 1"];
            }
            """.trimIndent()

        val graph = parseDot(dot)

        assertThat(graph.initial).containsExactly("1")
        assertThat(graph.steps).containsExactly(Step("1", "-2", "AddCoffee"))
        assertThat(graph.states.getValue("-2")["times"].items).containsExactly(TlaValue.StringValue("recent"))
    }

    @Test
    fun `TlcConfig writes the configuration with only the kept invariants`() {
        val config = TlcConfig.parse("SPECIFICATION Spec \\* the model\nCONSTANT MaxCount = 2\nINVARIANTS\n  A\n  B\n")

        val expected = "SPECIFICATION Spec CONSTANT MaxCount = 2\nINVARIANTS B\n"
        assertThat(config.withInvariants(listOf("B"))).isEqualTo(expected)
    }

    @Test
    fun `parseViolations takes the shortest path of each broken rule`() {
        val output =
            """
            Error: Invariant AtMostOne is violated.
            Error: The behavior up to this point is:
            State 1: <Initial predicate>
            /\ count = 0

            State 2: <AddCoffee line 19, col 5 to line 21, col 45 of module Broken>
            /\ count = 1

            State 3: <AddCoffee line 19, col 5 to line 21, col 45 of module Broken>
            /\ count = 2

            Error: Invariant AtMostOne is violated.
            Error: The behavior up to this point is:
            State 1: <Initial predicate>
            /\ count = 0

            State 2: <Correct line 30, col 9 to line 32, col 80 of module Broken>
            /\ count = 2

            Model checking completed. No error has been found.
            """.trimIndent()

        val rule = parseViolations(output).single()

        assertThat(rule.shortestPath.map { it.action }).containsExactly(null, "Correct")
    }

    @Test
    fun `parseViolations reads a rule that an initial state breaks`() {
        val output =
            "Error: Invariant Small is violated by the initial state:\n/\\ x = 5\n/\\ y = 1\n\nFinished computing"

        val rule = parseViolations(output).single()

        assertThat(rule.states.map { it["x"].int }).containsExactly(5L)
    }

    @Test
    fun `deadlock finds the shortest path to a state without a step out`() {
        val state = { x: Long -> ModelState(mapOf("x" to TlaValue.IntValue(x))) }
        val graph =
            StateGraph(
                states = mapOf("a" to state(0), "b" to state(1), "c" to state(2), "d" to state(3)),
                initial = setOf("a"),
                steps = listOf(Step("a", "b", "Up"), Step("b", "c", "Up"), Step("a", "c", "Jump"), Step("b", "d", "Up"))
            )

        val path = deadlock(graph)!!.shortestPath

        assertThat(path.map { it.action }).containsExactly(null, "Jump")
    }

    @Test
    fun `deadlock does not count a state with a step to itself`() {
        val state = ModelState(mapOf("x" to TlaValue.IntValue(0)))
        val graph = StateGraph(mapOf("a" to state), setOf("a"), listOf(Step("a", "a", "Stay")))

        assertThat(deadlock(graph)).isNull()
    }

    @Test
    fun `TlcConfig turns the check for deadlocks off with CHECK_DEADLOCK FALSE`() {
        assertThat(TlcConfig.parse("SPECIFICATION Spec\nCHECK_DEADLOCK FALSE\n").checksDeadlock).isFalse()
    }
}
