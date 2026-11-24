package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CountCorrection
import java.util.UUID

/**
 * The data port for the corrections. `CountCorrectionDataServiceImpl` in the data module implements it. Coffee Kitty
 * never changes or deletes a stored correction, because the corrections record who changed a coffee count and why. The
 * port therefore does not extend [CrudDataService] and offers neither an update nor a deletion.
 */
interface CountCorrectionDataService {
    /**
     * Stores [correction] under a new id.
     *
     * @param correction the correction to store, without an id
     * @return the stored correction with its id and timestamps
     * @throws IllegalArgumentException if [correction] has an id
     * @throws NotFoundException if the user or the admin of [correction] is not stored
     */
    fun insert(correction: CountCorrection): CountCorrection

    /**
     * Returns the corrections of the coffee count of the user with [userId], newest first. The list is empty for a
     * user without corrections and for an unknown user.
     *
     * @param userId the id of the user whose coffee count the admins corrected
     */
    fun getAllByUserId(userId: UUID): List<CountCorrection>
}
