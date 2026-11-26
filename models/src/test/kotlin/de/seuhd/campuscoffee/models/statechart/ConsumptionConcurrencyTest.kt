package de.seuhd.campuscoffee.models.statechart

import de.seuhd.campuscoffee.domain.implementation.CoffeeConsumptionServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * Checks the model `src/main/tla/ConsumptionConcurrency.tla` of concurrent requests and its assumption that
 * [CoffeeConsumptionServiceImpl] holds the row lock until the transaction ends. The requests of the model compute their
 * results with the operators of `Consumption.tla`, which ConsumptionConformanceTest replays on the service. That test
 * also checks the other assumption, that each function that changes a consumption reads it with the row lock. The
 * integration tests of the data module check that the lock makes a second transaction wait.
 */
class ConsumptionConcurrencyTest {
    @Test
    fun `with the row lock, concurrent requests end in a state that they also reach one after another`() {
        assertThat(check("ConsumptionConcurrency.cfg").broken).isEmpty()
    }

    @Test
    fun `concurrent requests without the row lock can lose the result of a request`() {
        val broken = check("ConsumptionConcurrencyNoLock.cfg").broken.map { it.name }
        assertThat(broken).containsExactly("Serializable")
    }

    @Test
    fun `each function that changes a consumption runs in one transaction`() {
        val changing = ConsumptionMapping.FUNCTIONS.values.toSet()
        val transactional =
            CoffeeConsumptionServiceImpl::class.java.declaredMethods
                .filter { it.name in changing }
                .filter { method -> method.annotations.any { it.annotationClass.java.name == TRANSACTIONAL } }
                .map { it.name }
        assertThat(transactional).containsExactlyInAnyOrderElementsOf(changing)
    }

    private fun check(config: String): CheckResult {
        val models = Path.of("src/main/tla")
        val work = Path.of("build/statecharts-test/$config").also { it.toFile().deleteRecursively() }
        val tlc = Tlc(javaCommand(), tla2tools())
        return tlc.check(models.resolve("ConsumptionConcurrency.tla"), models.resolve(config), work)
    }

    private companion object {
        const val TRANSACTIONAL = "org.springframework.transaction.annotation.Transactional"
    }
}
