package de.seuhd.campuscoffee.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * The domain model of the price per cup, which every coffee costs, in euro cents ([amountCents]). Coffee Kitty stores
 * at most one price. It inserts the first price, and every later change updates the stored price. The table
 * `coffee_prices` allows only one row, and `CoffeePriceDataService.upsert` throws a `DuplicationException` for a second
 * price without an id. Money is an integer number of cents, never a floating-point number of euros, so that sums have
 * no rounding errors.
 *
 * The data module assigns [id], [createdAt], and [updatedAt]. They are null on a price that is not stored yet.
 * [updatedAt] is the time of the most recent change of the price in UTC.
 */
data class CoffeePrice(
    override val id: UUID? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
    val amountCents: Int
) : DomainModel<UUID>
