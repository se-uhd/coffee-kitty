package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.CoffeeConsumptionEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CoffeeConsumptionRepository
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Data adapter that implements the [CoffeeConsumptionDataService] port on the `coffee_consumptions` table. The business
 * rules are in the domain services. Every user has exactly one consumption. A lookup that finds no consumption
 * therefore throws a [NotFoundException] for the user. [upsert] throws a `DuplicationException` for a second
 * consumption of a user (see [CrudDataServiceImpl]).
 */
@Service
class CoffeeConsumptionDataServiceImpl(
    repository: CoffeeConsumptionRepository,
    entityMapper: CoffeeConsumptionEntityMapper
) : CrudDataServiceImpl<CoffeeConsumption, CoffeeConsumptionEntity, CoffeeConsumptionRepository, UUID>(
        repository,
        entityMapper,
        CoffeeConsumption::class.java
    ),
    CoffeeConsumptionDataService {
    override fun getByUserId(userId: UUID): CoffeeConsumption = toDomainOrThrow(repository.findByUserId(userId), userId)

    /**
     * Returns the consumption of the user with [userId] and locks the consumption row until the transaction ends.
     * With propagation `MANDATORY`, a call outside a transaction throws an `IllegalTransactionStateException`. Without
     * a transaction, the lock would already end with the query, and the caller would store the new count without the
     * lock.
     *
     * @throws NotFoundException if no user has [userId]
     */
    @Transactional(propagation = Propagation.MANDATORY)
    override fun getByUserIdForUpdate(userId: UUID): CoffeeConsumption =
        toDomainOrThrow(repository.findForUpdateByUserId(userId), userId)

    /**
     * Maps [entity] to its domain object.
     *
     * @throws NotFoundException for the user with [userId] if [entity] is null
     */
    private fun toDomainOrThrow(
        entity: CoffeeConsumptionEntity?,
        userId: UUID
    ): CoffeeConsumption = entity?.let { mapper.fromEntity(it) } ?: throw NotFoundException(User::class.java, userId)
}
