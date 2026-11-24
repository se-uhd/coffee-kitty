package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.api.dtos.ConsumptionCorrectionDto
import de.seuhd.campuscoffee.api.dtos.ConsumptionDeltaDto
import de.seuhd.campuscoffee.api.dtos.CountCorrectionDto
import de.seuhd.campuscoffee.api.security.CurrentUserProvider
import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * System tests for correcting a coffee count and for listing the corrections (FR-8). The tests send their requests
 * through [ConsumptionRequests]. Before each test, the fixture user `maxmustermann` adds three coffees. The acting user
 * of the requests of FR-8 is the admin `jane_doe`, unless a test sets another acting user or leaves the header
 * `X-Acting-User` out.
 */
class CorrectionSystemTests : AbstractSystemTest() {
    private val note = "Double tap on 16.11."

    private fun userId(): UUID = fixtureUser(USER_LOGIN_NAME).persistedId

    @BeforeEach
    fun addThreeCoffees() {
        repeat(3) { ConsumptionRequests.applyDelta(userId(), ConsumptionDeltaDto(1)).requireStatus(200) }
    }

    private fun count(): Int = ConsumptionRequests.retrieveConsumption(userId()).body.count

    private fun corrections(): List<CountCorrectionDto> = ConsumptionRequests.retrieveCorrections(userId()).body

    @Test
    fun `correcting a coffee count as an admin returns 200 OK with the corrected count`() {
        val response = ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1, note = note))

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.loginName).isEqualTo(USER_LOGIN_NAME)
        assertThat(response.body.count).isEqualTo(1)
        assertThat(count()).isEqualTo(1)
    }

    @Test
    fun `correcting a coffee count without the acting user header returns 401 Unauthorized`() {
        val response =
            ConsumptionRequests.correctCount(
                userId(),
                ConsumptionCorrectionDto(total = 1, note = note),
                actingUser = null
            )

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.headers.getFirst("WWW-Authenticate"))
            .isEqualTo(CurrentUserProvider.AUTHENTICATION_CHALLENGE)
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @Test
    fun `correcting a coffee count to a negative count without the acting user header returns 401 Unauthorized`() {
        val response =
            ConsumptionRequests.correctCount(
                userId(),
                ConsumptionCorrectionDto(total = -1, note = note),
                actingUser = null
            )

        assertThat(response.status).isEqualTo(401)
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count as a user who is not an admin returns 403 Forbidden`() {
        val response =
            ConsumptionRequests.correctCount(
                userId(),
                ConsumptionCorrectionDto(total = 1, note = note),
                actingUser = USER_LOGIN_NAME
            )

        assertThat(response.status).isEqualTo(403)
        assertThat(response.error.errorCode).isEqualTo("ForbiddenException")
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @Test
    fun `correcting a coffee count to a negative count returns 400 Bad Request`() {
        val response = ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = -1, note = note))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 1_000_000])
    fun `correcting a coffee count to 0 or 1000000 returns 200 OK with that count`(total: Int) {
        val response = ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = total, note = note))

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.count).isEqualTo(total)
        assertThat(count()).isEqualTo(total)
    }

    @Test
    fun `correcting a coffee count to more than 1000000 returns 400 Bad Request`() {
        val response =
            ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1_000_001, note = note))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count without a total returns 400 Bad Request`() {
        val response = ConsumptionRequests.correctCount(userId(), mapOf("note" to note))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["   "])
    fun `correcting a coffee count without a note or with a blank note returns 400 Bad Request`(blankNote: String?) {
        val response = ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1, note = blankNote))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @Test
    fun `correcting a coffee count with a note of 500 characters returns 200 OK`() {
        val longNote = "a".repeat(500)

        val response = ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1, note = longNote))

        assertThat(response.status).isEqualTo(200)
        assertThat(corrections().single().note).isEqualTo(longNote)
    }

    @Test
    fun `correcting a coffee count with a note of 501 characters returns 400 Bad Request`() {
        val response =
            ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1, note = "a".repeat(501)))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count with a NUL character in the note returns 400 Bad Request`() {
        val response =
            ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1, note = "Double\u0000tap"))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting the coffee count of an unknown user returns 404 Not Found`() {
        val unknownUserId = UUID.randomUUID()

        val response =
            ConsumptionRequests.correctCount(unknownUserId, ConsumptionCorrectionDto(total = 1, note = note))

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `listing the corrections returns 200 OK with every correction, newest first`() {
        ConsumptionRequests.correctCount(userId(), ConsumptionCorrectionDto(total = 1, note = note)).requireStatus(200)
        ConsumptionRequests
            .correctCount(userId(), ConsumptionCorrectionDto(total = 2, note = "One coffee too many removed"))
            .requireStatus(200)

        val response = ConsumptionRequests.retrieveCorrections(userId())

        assertThat(response.status).isEqualTo(200)
        val corrections = response.body
        assertThat(corrections.map { Triple(it.countBefore, it.countAfter, it.note) })
            .containsExactly(Triple(1, 2, "One coffee too many removed"), Triple(3, 1, note))
        assertThat(corrections.map { it.adminLoginName }).containsOnly(ADMIN_LOGIN_NAME)
        assertThat(corrections.first().createdAt)
            .isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `listing the corrections of a user without corrections returns 200 OK with an empty list`() {
        val response = ConsumptionRequests.retrieveCorrections(userId())

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `listing the corrections for a non-UUID user id without the acting user header returns 401 Unauthorized`() {
        // Spring Security responds with 401 before Spring MVC can respond to the text not-a-uuid with 400
        val response = ConsumptionRequests.retrieveCorrections("not-a-uuid", actingUser = null)

        assertThat(response.status).isEqualTo(401)
    }

    @Test
    fun `listing the corrections without the acting user header returns 401 Unauthorized`() {
        val response = ConsumptionRequests.retrieveCorrections(userId(), actingUser = null)

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.headers.getFirst("WWW-Authenticate"))
            .isEqualTo(CurrentUserProvider.AUTHENTICATION_CHALLENGE)
    }

    @Test
    fun `listing the corrections as a user who is not an admin returns 403 Forbidden`() {
        val response = ConsumptionRequests.retrieveCorrections(userId(), actingUser = USER_LOGIN_NAME)

        assertThat(response.status).isEqualTo(403)
        assertThat(response.error.errorCode).isEqualTo("ForbiddenException")
    }

    @Test
    fun `listing the corrections of an unknown user returns 404 Not Found`() {
        val response = ConsumptionRequests.retrieveCorrections(UUID.randomUUID())

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    private companion object {
        /** The login name of the fixture user with the role ADMIN, who may correct coffee counts. */
        const val ADMIN_LOGIN_NAME = "jane_doe"

        /** The login name of the fixture user without the role ADMIN whose coffee count the tests correct. */
        const val USER_LOGIN_NAME = "maxmustermann"
    }
}
