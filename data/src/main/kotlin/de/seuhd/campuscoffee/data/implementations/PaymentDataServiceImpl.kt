package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.PaymentEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.PaymentEntity
import de.seuhd.campuscoffee.data.persistence.repositories.PaymentRepository
import de.seuhd.campuscoffee.domain.model.Payment
import de.seuhd.campuscoffee.domain.ports.data.PaymentDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Data adapter that implements the [PaymentDataService] port on the table `payments`. Business rules, such as the
 * limits of an amount and the rule that the kitty never goes negative, belong in a domain service. Coffee Kitty has no
 * domain service for the payments yet. [upsert] throws a `NotFoundException` for a deposit of a user who does not exist
 * (see [CrudDataServiceImpl]).
 */
@Service
class PaymentDataServiceImpl(
    repository: PaymentRepository,
    entityMapper: PaymentEntityMapper
) : CrudDataServiceImpl<Payment, PaymentEntity, PaymentRepository, UUID>(
        repository,
        entityMapper,
        Payment::class.java
    ),
    PaymentDataService {
    /**
     * Stores [domain] like [CrudDataServiceImpl.upsert], but first rejects a deposit of a user without an id. The
     * entity mapper looks up the user by id and would map a user without an id to null. The method would then store the
     * deposit as a kitty adjustment without an error.
     *
     * @throws IllegalArgumentException if [domain] has a user without an id
     */
    @Transactional
    override fun upsert(domain: Payment): Payment {
        val user = domain.user
        require(user == null || user.id != null) { "The user of a deposit must be stored." }
        return super.upsert(domain)
    }

    override fun getAllByUserId(userId: UUID): List<Payment> =
        repository.findAllByUserIdOrderByCreatedAtDesc(userId).map { mapper.fromEntity(it) }
}
