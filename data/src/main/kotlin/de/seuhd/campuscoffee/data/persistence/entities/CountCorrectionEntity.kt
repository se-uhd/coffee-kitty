package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * JPA entity of a correction, in which an [admin] set the coffee count of a [user] from [countBefore] to [countAfter]
 * and explained the correction in a [note]. The migration `V5__create_count_corrections_table.sql` creates the table
 * `count_corrections`. The column `note` has the width `CountCorrection.MAX_NOTE_LENGTH`.
 *
 * The foreign key `user_id` has `ON DELETE CASCADE`, because a correction belongs to its user, as a consumption does.
 * The foreign key `admin_id` has the default `NO ACTION`, because a correction does not belong to its admin.
 * PostgreSQL therefore does not delete an admin who corrected the coffee count of another user. JPA loads both users
 * together with the correction, because the read methods of the data adapter map the entity outside a transaction (see
 * `CrudDataServiceImpl`).
 *
 * V5 creates an index for each of the two foreign keys, because PostgreSQL does not create these indexes on its own.
 * The index on `user_id` and `created_at` is for `getAllByUserId`, which returns the corrections of a user, newest
 * first. Both indexes are also for deleting a user, because PostgreSQL then looks for the rows that refer to the user.
 */
@jakarta.persistence.Entity
@Table(name = "count_corrections")
class CountCorrectionEntity : Entity() {
    @field:ManyToOne
    @field:JoinColumn(name = "user_id", nullable = false)
    var user: UserEntity? = null

    @field:ManyToOne
    @field:JoinColumn(name = "admin_id", nullable = false)
    var admin: UserEntity? = null

    @field:Column(name = "count_before")
    var countBefore: Int? = null

    @field:Column(name = "count_after")
    var countAfter: Int? = null

    @field:Column(name = "note")
    var note: String? = null
}
