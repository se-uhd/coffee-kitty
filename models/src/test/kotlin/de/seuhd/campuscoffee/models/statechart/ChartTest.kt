package de.seuhd.campuscoffee.models.statechart

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class ChartTest {
    private val view =
        object : ChartView {
            override val title = "Counter"
            override val rules = mapOf("Small" to "The count stays small.")

            override fun state(state: ModelState) =
                if (state["count"].int == 0L) listOf("Zero") else listOf("Positive", "count ${state["count"].int}")

            override fun step(
                action: String,
                from: ModelState,
                to: ModelState
            ) = if (action == "Reset") null else "a user presses $action"
        }

    private fun state(count: Long) = ModelState(mapOf("count" to TlaValue.IntValue(count)))

    private val graph =
        StateGraph(
            states = mapOf("a" to state(0), "b" to state(1), "c" to state(2)),
            initial = setOf("a"),
            steps =
                listOf(
                    Step("a", "b", "Up"),
                    Step("b", "c", "Up"),
                    Step("c", "b", "Down"),
                    Step("c", "a", "Reset")
                )
        )

    @Test
    fun `buildChart hides the steps that the view hides`() {
        val chart = buildChart(view, graph, emptyList())

        assertThat(chart.hidden).containsExactly("Reset")
    }

    @Test
    fun `buildChart marks a state whose model states all break a rule`() {
        val broken = BrokenRule("Small", setOf(state(2)), listOf(TraceStep(null, state(2))))

        val chart = buildChart(view, graph, listOf(broken))

        assertThat(chart.states.single { it.path.last() == "count 2" }.mark).isEqualTo(Mark.BREAKS)
    }

    @Test
    fun `toPlantUml draws an arrow back to an earlier state upwards, with an up arrow before its label`() {
        val source = toPlantUml(buildChart(view, graph, emptyList()), view.rules)

        assertThat(source).contains("s2 -up-> s1 : \u2191 a user presses Down")
        assertThat(source).contains("s1 --> s2 : \u2193 a user presses Up")
    }

    @Test
    fun `toPlantUml nests the states of a parent in a composite state`() {
        val source = toPlantUml(buildChart(view, graph, emptyList()), view.rules)

        val composite = "state \"Positive\" as p0 {\n  state \"count 1\" as s1\n  state \"count 2\" as s2\n}"
        assertThat(source).contains(composite)
    }

    @Test
    fun `toPlantUml draws every state in each panel, with the start and the arrows of the actions of the panel`() {
        val panels = mapOf("Steps up" to setOf("Up"), "Steps down" to setOf("Down"))
        val source = toPlantUml(buildChart(viewWith(panels), graph, emptyList()), view.rules)

        assertThat(
            source
        ).contains("state \"Steps up\" as q0 {", "  [*] --> q0s0", "  q0s0 --> q0s1 : \u2193 a user presses Up")
        assertThat(
            source
        ).contains("state \"Steps down\" as q1 {", "  [*] --> q1s0", "  q1s2 -up-> q1s1 : \u2191 a user presses Down")
        assertThat(source).doesNotContain("q0s2 -up-> q0s1", "q1s0 --> q1s1")
    }

    @Test
    fun `buildChart fails when no panel lists the action of an arrow`() {
        assertThatThrownBy { buildChart(viewWith(mapOf("Steps up" to setOf("Up"))), graph, emptyList()) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("Down")
    }

    private fun viewWith(panels: Map<String, Set<String>>): ChartView =
        object : ChartView by view {
            override val panels = panels
        }
}
