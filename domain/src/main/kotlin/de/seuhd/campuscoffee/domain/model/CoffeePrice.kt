package de.seuhd.campuscoffee.domain.model

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of the price per cup in euro cents. Coffee Kitty stores at most one price. It inserts the first
 * price, and every later change updates the stored price. The table `coffee_prices` allows only one row, and
 * `CoffeePriceDataService.upsert` throws a `DuplicationException` for a second price without an id. [updatedAt] is the
 * time of the most recent change of the price in UTC.
 */
data class CoffeePrice(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    @field:Min(value = 0, message = AMOUNT_MESSAGE)
    @field:Max(value = MAX_AMOUNT_CENTS.toLong(), message = AMOUNT_MESSAGE)
    val amountCents: Int
) : DomainModel<UUID> {
    /** The upper limit of a price per cup and the message for a price outside the limits. */
    companion object {
        /**
         * The highest price per cup in euro cents, 1,000.00 €. The group lead set this upper limit for every single
         * amount of money (FR-17).
         */
        const val MAX_AMOUNT_CENTS = 100_000

        /** The message of the error response for a price outside the limits. */
        const val AMOUNT_MESSAGE = "The price per cup must be from 0 to $MAX_AMOUNT_CENTS euro cents."
    }
}
