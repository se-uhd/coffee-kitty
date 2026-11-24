package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.persistedId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

/**
 * System tests for an undo and for the undo status after the grace period, with the system clock. The class sets the
 * grace period, `campus-coffee.consumption.grace-period`, to zero, so that the grace period of a coffee has passed when
 * the next request arrives. If the app did not read this key, the default grace period of 5 minutes would apply. The
 * undo would then succeed, the undo status would be undoable, and both tests would fail. The acceptance scenarios in
 * `consumption.feature` check the end of the default grace period with the `SettableClock`.
 */
@TestPropertySource(properties = ["campus-coffee.consumption.grace-period=0s"])
class UndoWithoutGracePeriodSystemTests : AbstractSystemTest() {
    @Test
    fun `undoing a coffee with a grace period of zero returns 409 Conflict`() {
        val userId = fixtureUser("maxmustermann").persistedId
        consumptionRequests.addCoffee(userId).requireStatus(200)

        val response = consumptionRequests.undo(userId)

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("ConflictException")
        assertThat(consumptionRequests.retrieveConsumption(userId).body.count).isEqualTo(1)
    }
}
