package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.model.Payment
import java.util.UUID

/**
 * Data port for the payments, that is, the deposits and the kitty adjustments. It adds a lookup by user to the
 * operations of [CrudDataService]. `PaymentDataServiceImpl` in the data module implements it. [getAll] returns the
 * payments, ordered by creation time.
 */
interface PaymentDataService : CrudDataService<Payment, UUID> {
    /**
     * Returns the deposits of the user with [userId], newest first. The list is empty for a user without deposits and
     * for an unknown user. It never contains a kitty adjustment, because a kitty adjustment has no user.
     *
     * @param userId the id of the user who paid the deposits
     */
    fun getAllByUserId(userId: UUID): List<Payment>
}
