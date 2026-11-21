package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource

/**
 * System test for an undo after the grace period, with the system clock. The test sets the grace period,
 * `campus-coffee.consumption.grace-period`, to zero, so that the grace period of a coffee has passed when the next
 * request arrives. The test fails when the app does not read the grace period from this key, because the default
 * grace period of 5 minutes would accept the undo. The acceptance scenarios in `consumption.feature` check the end of
 * the default grace period with a clock that they set.
 */
@TestPropertySource(properties = ["campus-coffee.consumption.grace-period=0s"])
class UndoWithoutGracePeriodSystemTests : AbstractSystemTest() {
    @Test
    fun `undoing a coffee with a grace period of zero returns 409 Conflict and keeps the count at 1`() {
        val userId = fixtureUser("maxmustermann").persistedId
        ConsumptionRequests.applyDelta(userId, ConsumptionDeltaDto(1)).requireStatus(200)

        val response = ConsumptionRequests.undo(userId)

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("ConflictException")
        assertThat(response.error.message).isEqualTo("The grace period to undo this coffee has passed.")
        assertThat(ConsumptionRequests.retrieveConsumption(userId).body.count).isEqualTo(1)
    }
}
