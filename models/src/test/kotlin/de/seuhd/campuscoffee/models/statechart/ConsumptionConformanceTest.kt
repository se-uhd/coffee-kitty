package de.seuhd.campuscoffee.models.statechart

import de.seuhd.campuscoffee.domain.exceptions.ConflictException
import de.seuhd.campuscoffee.domain.exceptions.ForbiddenException
import de.seuhd.campuscoffee.domain.exceptions.StaleDataException
import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.implementation.CoffeeConsumptionServiceImpl
import de.seuhd.campuscoffee.domain.implementation.ConsumptionProperties
import de.seuhd.campuscoffee.domain.implementation.DomainValidator
import de.seuhd.campuscoffee.domain.implementation.GracePeriod
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.ports.api.CoffeeConsumptionService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.nio.file.Path
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.io.path.readText
import kotlin.random.Random

/**
 * Replays the model `src/main/tla/Consumption.tla` on [CoffeeConsumptionServiceImpl], with the larger bound of
 * `ConsumptionReplay.cfg`, as the section Link the model to the code of `.claude/skills/statechart/SKILL.md` describes.
 * The test maps each stored coffee time to "recent" when its grace period is open and to "old" otherwise.
 */
class ConsumptionConformanceTest {
    private val grace: Duration = Duration.ofMinutes(5)
    private val userId: UUID = UUID(0L, 1L)
    private val user = TestFixtures.regularUser().copy(id = userId)
    private val admin = TestFixtures.admin().copy(id = UUID(0L, 2L))
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @TestFactory
    fun `each step of the model leads the service to the state after the step`(): List<DynamicTest> =
        graph.steps.map { step ->
            val from = graph.states.getValue(step.from)
            val to = graph.states.getValue(step.to)
            dynamicTest("${step.action}: ${describe(from)} -> ${describe(to)}") {
                for (counts in countsFor(step.action)) {
                    for (timing in Timing.entries.filter { step.action != CLOCK_ACTION || it.distinctTimes }) {
                        for (input in inputs(step.action, from, to, counts)) {
                            val app = App(from, timing, counts)
                            app.perform(step.action, input)
                            assertThat(app.modelState())
                                .describedAs("with %s counts, %s times, and %s", counts, timing, input)
                                .isEqualTo(to)
                        }
                    }
                }
            }
        }

    @TestFactory
    fun `the service reads the consumption with the row lock in each step that changes it`(): List<DynamicTest> =
        changingSteps().map { step ->
            val from = graph.states.getValue(step.from)
            val to = graph.states.getValue(step.to)
            dynamicTest("${step.action}: ${describe(from)}") {
                val counts = countsFor(step.action).first()
                val app = App(from, Timing.SPACED, counts)
                app.perform(step.action, inputs(step.action, from, to, counts).first())
                assertThat(app.reads()).containsExactly("getByUserIdForUpdate")
            }
        }

    @TestFactory
    fun `undo succeeds exactly in the states in which the model has an undo step`(): List<DynamicTest> =
        graph.states.map { (id, state) ->
            val undoable = graph.steps.any { it.from == id && it.action == "Undo" }
            dynamicTest("${describe(state)}, undo ${if (undoable) "succeeds" else "fails"}") {
                for (counts in Counts.entries) {
                    for (timing in Timing.entries) {
                        val app = App(state, timing, counts)
                        val case = "with $counts counts and $timing times"
                        if (undoable) {
                            assertDoesNotThrow(case) { app.service.undo(userId) }
                        } else {
                            assertThrows<ConflictException>(case) { app.service.undo(userId) }
                        }
                    }
                }
            }
        }

    @Test
    fun `the model with the bound of the replay breaks no rule`() {
        assertThat(result.broken.map { it.name }).isEmpty()
    }

    @TestFactory
    fun `random walks keep the service in a state of the model`(): List<DynamicTest> =
        (1..WALKS).map { walk ->
            val counts = Counts.entries[walk % Counts.entries.size]
            dynamicTest("walk $walk with $counts counts") {
                val random = Random(walk)
                var node = graph.initial.single()
                val app = App(graph.states.getValue(node), Timing.SPACED, counts)
                val path = mutableListOf<String>()
                repeat(WALK_LENGTH) {
                    app.passTime(random)
                    assertThat(app.modelState()).describedAs("time passed after %s", path).isEqualTo(graph.states[node])
                    val step =
                        graph.steps
                            .filter { it.from == node && counts in countsFor(it.action) }
                            .filter { it.action != "AddCoffee" || app.addsANewTime() }
                            .random(random)
                    val to = graph.states.getValue(step.to)
                    val input = inputs(step.action, graph.states.getValue(node), to, counts).random(random)
                    path += "${step.action} $input -> ${describe(to)}"
                    app.perform(step.action, input)
                    assertThat(app.modelState()).describedAs("after %s", path).isEqualTo(to)
                    node = step.to
                }
            }
        }

    @Test
    fun `each function of CoffeeConsumptionService is an action of the model or a listed query`() {
        val functions =
            CoffeeConsumptionService::class.java.declaredMethods
                .filterNot { it.isSynthetic }
                .map { it.name }
                .toSet()
        assertThat(functions).isEqualTo(ConsumptionMapping.FUNCTIONS.values.toSet() + ConsumptionMapping.QUERIES)
    }

    @Test
    fun `each action of the model has a function of the service or moves the clock`() {
        val definition = MODEL.readText().substringAfter("\nNext ==").substringBefore("\n\n")
        val actions =
            definition
                .split("\\/")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
        assertThat(actions).isEqualTo(ConsumptionMapping.FUNCTIONS.keys + CLOCK_ACTION)
    }

    private fun describe(state: ModelState) =
        "count ${state["count"].int}, times ${state["times"].items.joinToString(", ", "[", "]") { it.string }}"

    private fun changingSteps() =
        graph.steps
            .filter { it.action in ConsumptionMapping.FUNCTIONS && it.from != it.to }
            .distinctBy { it.from to it.action }

    private fun countsFor(action: String) =
        when (action) {
            "RefuseCoffee" -> listOf(Counts.HIGH)
            "Correct" -> listOf(Counts.LOW)
            else -> Counts.entries
        }

    private fun concreteCount(
        modelCount: Long,
        counts: Counts
    ): Int = modelCount.toInt() + if (counts == Counts.HIGH) Int.MAX_VALUE - MAX_COUNT else 0

    private data class Input(
        val count: Int = 0,
        val note: String? = "",
        val delta: Int = 1,
        val byAdmin: Boolean = true,
        val expectedCount: Int? = null
    ) {
        override fun toString() =
            "count $count, note of ${note?.length} characters, delta $delta, ${if (byAdmin) "admin" else "user"}, " +
                "expected count $expectedCount"
    }

    private fun inputs(
        action: String,
        from: ModelState,
        to: ModelState,
        counts: Counts
    ): List<Input> {
        val current = concreteCount(from["count"].int, counts)
        val target = concreteCount(to["count"].int, counts)
        val refused =
            INVALID_NOTES.map { Input(current, it) } +
                listOf(-1, CountCorrection.MAX_CORRECTED_COUNT + 1).map { Input(it, VALID_NOTES.last()) }
        return when (action) {
            "Correct" -> {
                VALID_NOTES.map { Input(target, it) } +
                    Input(target, VALID_NOTES.last(), expectedCount = current)
            }

            "RefuseCorrection" -> {
                refused
            }

            "RefuseStaleCorrection" -> {
                listOf(
                    Input(
                        0,
                        VALID_NOTES.last(),
                        expectedCount =
                            if (current ==
                                0
                            ) {
                                1
                            } else {
                                current - 1
                            }
                    )
                )
            }

            "RefuseCorrectionByUser" -> {
                listOf(Input(current, VALID_NOTES.first(), byAdmin = false))
            }

            "RefuseDelta" -> {
                REFUSED_DELTAS.map { Input(delta = it) }
            }

            else -> {
                listOf(Input())
            }
        }
    }

    private enum class Counts {
        /**
         * The counts of the model. The service refuses a coffee only at the largest count that it stores, and it
         * corrects a count only up to 1,000,000, so the test replays `RefuseCoffee` only in [HIGH] and `Correct` here.
         */
        LOW,

        /** The counts of the model moved up, so that the largest count of the model is `Int.MAX_VALUE`. */
        HIGH
    }

    private enum class Timing(
        val distinctTimes: Boolean
    ) {
        /** Old coffees with grace periods that ended minutes ago, recent coffees 10 seconds apart. */
        SPACED(true),

        /**
         * The clock at the last instant of the grace period of the oldest recent coffee and 1 µs after the grace period
         * of the newest old coffee.
         */
        BOUNDARY(true),

        /** All recent coffees at one instant, so that no move of the clock ends only one grace period. */
        SAME_INSTANT(false),

        /** Like [SPACED], with a clock that shows nanoseconds and times truncated to microseconds. */
        NANOS(true)
    }

    private inner class App(
        state: ModelState,
        timing: Timing,
        private val counts: Counts
    ) {
        private val start = concretize(state, timing, counts)
        val clock = MutableClock(start.second)
        private val gracePeriod = GracePeriod(ConsumptionProperties(grace), clock)
        private var stored = start.first
        private val consumptions: CoffeeConsumptionDataService = mock()
        private val corrections: CountCorrectionDataService = mock()
        val service =
            CoffeeConsumptionServiceImpl(consumptions, corrections, gracePeriod, clock, DomainValidator(validator))

        init {
            whenever(consumptions.getByUserId(userId)).thenAnswer { stored }
            whenever(consumptions.getByUserIdForUpdate(userId)).thenAnswer { stored }
            whenever(consumptions.upsert(any())).thenAnswer { invocation ->
                stored = invocation.arguments[0] as CoffeeConsumption
                stored
            }
            whenever(corrections.insert(any())).thenAnswer { it.arguments[0] as CountCorrection }
            assertThat(modelState())
                .describedAs("%s with %s counts and %s times", describe(state), counts, timing)
                .isEqualTo(state)
        }

        fun perform(
            action: String,
            input: Input
        ) {
            when (action) {
                "AddCoffee" -> service.applyDelta(userId, 1)
                "RefuseCoffee" -> refuse<ConflictException>(input) { service.applyDelta(userId, 1) }
                "RefuseDelta" -> refuse<ValidationException>(input) { service.applyDelta(userId, input.delta) }
                "Undo" -> service.undo(userId)
                "Correct" -> correct(input)
                "RefuseCorrection" -> refuse<ValidationException>(input) { correct(input) }
                "RefuseStaleCorrection" -> refuse<StaleDataException>(input) { correct(input) }
                "RefuseCorrectionByUser" -> refuse<ForbiddenException>(input) { correct(input) }
                CLOCK_ACTION -> endOldestGracePeriod()
                else -> fail("The model has the action $action, which this test does not map to the service.")
            }
        }

        private fun correct(input: Input) {
            service.correctCount(
                userId,
                input.count,
                input.note,
                if (input.byAdmin) admin else user,
                input.expectedCount
            )
        }

        private inline fun <reified T : Throwable> refuse(
            input: Input,
            noinline call: () -> Unit
        ) {
            assertThrows<T>("with $counts counts and $input") { call() }
        }

        /** Returns the functions of the data port with which the service read the consumption. */
        fun reads(): List<String> =
            mockingDetails(consumptions).invocations.map { it.method.name }.filter { it.startsWith("getByUserId") }

        private fun endOldestGracePeriod() {
            val oldestOpen =
                stored.coffeeTimes.firstOrNull { gracePeriod.isOpen(it) } ?: fail("No grace period is open.")
            clock.now = oldestOpen.plus(grace).plusNanos(MICROSECOND)
        }

        /**
         * Moves the clock by a random amount, at least 1 µs when possible, that ends no grace period.
         *
         * @param random the random generator of the walk
         */
        fun passTime(random: Random) {
            val oldestOpen = stored.coffeeTimes.firstOrNull { gracePeriod.isOpen(it) }
            val max = oldestOpen?.let { Duration.between(clock.now, it.plus(grace)).toNanos() } ?: (grace.toNanos() * 2)
            clock.now = clock.now.plusNanos(random.nextLong(minOf(MICROSECOND, max), max + 1))
        }

        /** Returns whether a coffee added now gets a time that differs from the time of the newest coffee. */
        fun addsANewTime() = stored.coffeeTimes.lastOrNull() != clock.now.truncatedTo(ChronoUnit.MICROS)

        fun modelState(): ModelState {
            val times = stored.coffeeTimes.map { TlaValue.StringValue(if (gracePeriod.isOpen(it)) "recent" else "old") }
            val modelCount = stored.count.toLong() - concreteCount(0, counts)
            return ModelState(mapOf("count" to TlaValue.IntValue(modelCount), "times" to TlaValue.SeqValue(times)))
        }
    }

    private fun concretize(
        state: ModelState,
        timing: Timing,
        counts: Counts
    ): Pair<CoffeeConsumption, LocalDateTime> {
        val kinds = state["times"].items.map { it.string }
        val old = kinds.count { it == "old" }
        val recent = kinds.size - old
        assertThat(kinds).isEqualTo(List(old) { "old" } + List(recent) { "recent" })
        val now = TestFixtures.FIXED_TIME.plusNanos(if (timing == Timing.NANOS) CLOCK_NANOS else 0)
        val end = now.minus(grace)
        val times =
            when (timing) {
                Timing.SPACED, Timing.NANOS -> {
                    spaced(old, recent, now)
                }

                Timing.BOUNDARY -> {
                    atBoundary(old, recent, end)
                }

                Timing.SAME_INSTANT -> {
                    List(old) { end.minusMinutes(1) } +
                        List(recent) { now.minusSeconds(SPACING_SECONDS) }
                }
            }
        val consumption =
            CoffeeConsumption(
                id = UUID(0L, 500L),
                user = user,
                count = concreteCount(state["count"].int, counts),
                coffeeTimes = times.map { it.truncatedTo(ChronoUnit.MICROS) }
            )
        return consumption to now
    }

    private fun spaced(
        old: Int,
        recent: Int,
        now: LocalDateTime
    ): List<LocalDateTime> =
        List(old) { now.minus(grace).minusMinutes((old - it).toLong()) } +
            List(recent) { now.minusSeconds(SPACING_SECONDS * (recent - 1 - it)) }

    private fun atBoundary(
        old: Int,
        recent: Int,
        end: LocalDateTime
    ): List<LocalDateTime> =
        List(old) { end.minusNanos(MICROSECOND).minusMinutes((old - 1 - it).toLong()) } +
            List(recent) { end.plusSeconds(SPACING_SECONDS * it) }

    private class MutableClock(
        var now: LocalDateTime
    ) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = this

        override fun instant(): Instant = now.toInstant(ZoneOffset.UTC)
    }

    private companion object {
        const val CLOCK_ACTION = "GracePeriodEnds"
        const val SPACING_SECONDS = 10L
        const val MICROSECOND = 1_000L
        const val CLOCK_NANOS = 123_456_789L
        const val WALKS = 100
        const val WALK_LENGTH = 40

        val MODEL: Path = Path.of("src/main/tla/Consumption.tla")
        val CONFIG: Path = Path.of("src/main/tla/ConsumptionReplay.cfg")

        /** The largest count of the model in [CONFIG]. */
        val MAX_COUNT: Int = Regex("""MaxCount\s*=\s*(\d+)""").find(CONFIG.readText())!!.groupValues[1].toInt()

        /** Notes that a correction accepts: none, a blank one, a short one, and one of the largest length. */
        val VALID_NOTES = listOf(null, " \u00A0", "x".repeat(CountCorrection.MAX_NOTE_LENGTH), "Double tap on 16.11.")

        /** Notes that CountCorrection refuses: one that is too long and one with a NUL character. */
        val INVALID_NOTES = listOf("x".repeat(CountCorrection.MAX_NOTE_LENGTH + 1), "Double\u0000tap")

        /** Deltas that the service refuses, from the classes of the model. */
        val REFUSED_DELTAS = listOf(-1, 0, 2)

        val result: CheckResult by lazy {
            val work = Path.of("build/statecharts-test/Consumption").also { it.toFile().deleteRecursively() }
            Tlc(javaCommand(), tla2tools()).check(MODEL, CONFIG, work)
        }

        val graph: StateGraph get() = result.graph
    }
}
