package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

/**
 * The JPA entity of the balance of one user in euro cents ([balanceCents]). A negative balance means that the user owes
 * the kitty money, and a positive balance that the kitty owes the user money. The migration
 * `V11__create_balance_tables.sql` creates the table `user_balance`. Coffee Kitty has no domain model, port, or adapter
 * for balances yet, and no code writes the table.
 * [UserBalanceRepository][de.seuhd.campuscoffee.data.persistence.repositories.UserBalanceRepository] already reads a
 * balance with a lock.
 *
 * Coffee Kitty does not store the time and the price of every past coffee, so it cannot compute a balance from the
 * stored data afterwards. A domain service has to change the balance in the same transaction when it adds or undoes a
 * coffee, corrects a coffee count, or stores a deposit or an expense.
 *
 * The id of the row is the id of the user ([userId]), and the foreign key `user_id` has `ON DELETE CASCADE`. The class
 * does not extend [Entity], because the table has neither a random id nor timestamps. `EntityMapper` and
 * `CrudDataServiceImpl` accept only subclasses of [Entity]. A data adapter for this table therefore reads and writes it
 * through the repository. The application assigns the id, so `save` on a new instance first runs a SELECT for the row
 * and then an INSERT (see `PersistableEntity`).
 */
@jakarta.persistence.Entity
@Table(name = "user_balance")
class UserBalanceEntity {
    @field:Id
    @field:Column(name = "user_id")
    var userId: UUID? = null

    @field:Column(name = "balance_cents")
    var balanceCents: Long = 0
}
