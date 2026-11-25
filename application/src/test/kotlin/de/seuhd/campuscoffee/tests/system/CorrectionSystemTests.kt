package de.seuhd.campuscoffee.tests.system

import de.seuhd.campuscoffee.domain.model.persistedId
import de.seuhd.campuscoffee.tests.ConsumptionRequests
import de.seuhd.campuscoffee.tests.CorrectionJson
import de.seuhd.campuscoffee.tests.SystemTestUtils.ADMIN_LOGIN_NAME
import de.seuhd.campuscoffee.tests.SystemTestUtils.AUTHENTICATION_CHALLENGE
import de.seuhd.campuscoffee.tests.parseUtc
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * The system tests for correcting a coffee count and for listing the corrections (FR-8). The tests send their requests
 * through [ConsumptionRequests]. Before each test, the user fixture `maxmustermann` adds three coffees. The acting user
 * of the requests of FR-8 is the admin `jane_doe`, unless a test sets another acting user or leaves the header
 * `X-Acting-User` out.
 */
class CorrectionSystemTests : AbstractSystemTest() {
    private val note = "Double tap on 16.11."

    private fun userId(): UUID = userFixture(USER_LOGIN_NAME).persistedId

    @BeforeEach
    fun addThreeCoffees() {
        repeat(3) { consumptionRequests.addCoffee(userId()).requireStatus(200) }
    }

    private fun count(): Int = consumptionRequests.retrieveConsumption(userId()).body.count

    private fun corrections(): List<CorrectionJson> = consumptionRequests.retrieveCorrections(userId()).body

    @Test
    fun `correcting a coffee count as an admin returns 200 OK with the corrected count`() {
        val response = consumptionRequests.correctCount(userId(), 1, note)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.loginName).isEqualTo(USER_LOGIN_NAME)
        assertThat(response.body.count).isEqualTo(1)
        assertThat(count()).isEqualTo(1)
    }

    @Test
    fun `correcting a coffee count without the acting user header returns 401 Unauthorized`() {
        val response =
            consumptionRequests.correctCount(
                userId(),
                1,
                note,
                actingUser = null
            )

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.headers.getFirst("WWW-Authenticate"))
            .isEqualTo(AUTHENTICATION_CHALLENGE)
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @Test
    fun `correcting a coffee count without a total and without the acting user header returns 401 Unauthorized`() {
        // With an acting user, the body without a total returns 400. Without the rule of SecurityConfig for this
        // endpoint, Spring MVC would read the body first, and this request would return 400 as well.
        val response = consumptionRequests.correctCount(userId(), mapOf("note" to note), actingUser = null)

        assertThat(response.status).isEqualTo(401)
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count as a user who is not an admin returns 403 Forbidden`() {
        val response =
            consumptionRequests.correctCount(
                userId(),
                1,
                note,
                actingUser = USER_LOGIN_NAME
            )

        assertThat(response.status).isEqualTo(403)
        assertThat(response.error.errorCode).isEqualTo("ForbiddenException")
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @Test
    fun `correcting a coffee count to a negative count returns 400 Bad Request`() {
        val response = consumptionRequests.correctCount(userId(), -1, note)

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(count()).isEqualTo(3)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, Int.MAX_VALUE])
    fun `correcting a coffee count to 0 or 2147483647 returns 200 OK with that count`(total: Int) {
        val response = consumptionRequests.correctCount(userId(), total, note)

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body.count).isEqualTo(total)
        assertThat(count()).isEqualTo(total)
    }

    @Test
    fun `correcting a coffee count to more than 2147483647 returns 400 Bad Request`() {
        val response = consumptionRequests.correctCount(userId(), mapOf("total" to 2_147_483_648, "note" to note))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `adding a coffee at a coffee count of 2147483647 returns 409 Conflict`() {
        consumptionRequests.correctCount(userId(), Int.MAX_VALUE, note).requireStatus(200)

        val response = consumptionRequests.addCoffee(userId())

        assertThat(response.status).isEqualTo(409)
        assertThat(response.error.errorCode).isEqualTo("ConflictException")
        assertThat(count()).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `correcting a coffee count to its current value returns 200 OK and stores the correction`() {
        val response = consumptionRequests.correctCount(userId(), 3, note)

        assertThat(response.status).isEqualTo(200)
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).hasSize(1)
    }

    @Test
    fun `correcting a coffee count with a blank note as a user who is not an admin returns 403 Forbidden`() {
        val response = consumptionRequests.correctCount(userId(), 2, "   ", actingUser = USER_LOGIN_NAME)

        assertThat(response.status).isEqualTo(403)
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count without a total returns 400 Bad Request`() {
        val response = consumptionRequests.correctCount(userId(), mapOf("note" to note))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count without a note returns 400 Bad Request with the error code BadRequest`() {
        val response = consumptionRequests.correctCount(userId(), mapOf("total" to 1))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("BadRequest")
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   ", "\u00A0"])
    fun `correcting a coffee count with a blank note returns 400 Bad Request`(blankNote: String) {
        val response = consumptionRequests.correctCount(userId(), 1, blankNote)

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(count()).isEqualTo(3)
        assertThat(corrections()).isEmpty()
    }

    @Test
    fun `correcting a coffee count with a note of 500 characters returns 200 OK`() {
        val longNote = "a".repeat(500)

        val response = consumptionRequests.correctCount(userId(), 1, longNote)

        assertThat(response.status).isEqualTo(200)
        assertThat(corrections().single().note).isEqualTo(longNote)
    }

    @Test
    fun `correcting a coffee count with a note of 501 characters returns 400 Bad Request`() {
        val response =
            consumptionRequests.correctCount(userId(), 1, "a".repeat(501))

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting a coffee count with a NUL character in the note returns 400 Bad Request`() {
        val response =
            consumptionRequests.correctCount(userId(), 1, "Double\u0000tap")

        assertThat(response.status).isEqualTo(400)
        assertThat(response.error.errorCode).isEqualTo("ValidationException")
        assertThat(count()).isEqualTo(3)
    }

    @Test
    fun `correcting the coffee count of an unknown user returns 404 Not Found`() {
        val unknownUserId = UUID.randomUUID()

        val response =
            consumptionRequests.correctCount(unknownUserId, 1, note)

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    @Test
    fun `listing the corrections returns 200 OK with every correction, newest first`() {
        consumptionRequests.correctCount(userId(), 1, note).requireStatus(200)
        consumptionRequests.correctCount(userId(), 2, "One coffee too many removed").requireStatus(200)

        val response = consumptionRequests.retrieveCorrections(userId())

        assertThat(response.status).isEqualTo(200)
        val corrections = response.body
        assertThat(corrections.map { Triple(it.countBefore, it.countAfter, it.note) })
            .containsExactly(Triple(1, 2, "One coffee too many removed"), Triple(3, 1, note))
        assertThat(corrections.map { it.adminLoginName }).containsOnly(ADMIN_LOGIN_NAME)
        assertThat(parseUtc(corrections.first().createdAt))
            .isCloseTo(LocalDateTime.now(ZoneOffset.UTC), within(1, ChronoUnit.MINUTES))
    }

    @Test
    fun `listing the corrections of a user without corrections returns 200 OK with an empty list`() {
        val response = consumptionRequests.retrieveCorrections(userId())

        assertThat(response.status).isEqualTo(200)
        assertThat(response.body).isEmpty()
    }

    @Test
    fun `listing the corrections for a non-UUID user id without the acting user header returns 401 Unauthorized`() {
        // Spring Security responds with 401 before Spring MVC can respond to the text not-a-uuid with 400
        val response = consumptionRequests.retrieveCorrections("not-a-uuid", actingUser = null)

        assertThat(response.status).isEqualTo(401)
    }

    @Test
    fun `listing the corrections without the acting user header returns 401 Unauthorized`() {
        val response = consumptionRequests.retrieveCorrections(userId(), actingUser = null)

        assertThat(response.status).isEqualTo(401)
        assertThat(response.error.errorCode).isEqualTo("Unauthorized")
        assertThat(response.headers.getFirst("WWW-Authenticate"))
            .isEqualTo(AUTHENTICATION_CHALLENGE)
    }

    @Test
    fun `listing the corrections as a user who is not an admin returns 403 Forbidden`() {
        val response = consumptionRequests.retrieveCorrections(userId(), actingUser = USER_LOGIN_NAME)

        assertThat(response.status).isEqualTo(403)
        assertThat(response.error.errorCode).isEqualTo("ForbiddenException")
    }

    @Test
    fun `listing the corrections of an unknown user returns 404 Not Found`() {
        val response = consumptionRequests.retrieveCorrections(UUID.randomUUID())

        assertThat(response.status).isEqualTo(404)
        assertThat(response.error.errorCode).isEqualTo("NotFoundException")
    }

    private companion object {
        /** The login name of the user fixture without the role ADMIN whose coffee count the tests correct. */
        const val USER_LOGIN_NAME = "maxmustermann"
    }
}
