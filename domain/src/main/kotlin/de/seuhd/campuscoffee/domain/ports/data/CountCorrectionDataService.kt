package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.model.CountCorrection
import java.util.UUID

/**
 * Data port for the corrections, with a lookup by user in addition to the operations of [CrudDataService].
 * `CountCorrectionDataServiceImpl` in the data module implements it. [getAll] returns the corrections, ordered by
 * creation time.
 */
interface CountCorrectionDataService : CrudDataService<CountCorrection, UUID> {
    /**
     * Returns the corrections of the coffee count of the user with [userId], newest first. The list is empty for a
     * user without corrections and for an unknown user.
     *
     * @param userId the id of the user whose coffee count the admins corrected
     */
    fun getAllByUserId(userId: UUID): List<CountCorrection>
}
