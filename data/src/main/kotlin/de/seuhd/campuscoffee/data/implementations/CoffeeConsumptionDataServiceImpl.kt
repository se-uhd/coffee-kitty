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
 * The data adapter for the [CoffeeConsumptionDataService] port over the `coffee_consumptions` table. The business rules
 * are in the domain services. A lookup for an unknown user throws a [NotFoundException] for `User`, not for the
 * consumption, because every user has exactly one consumption. [upsert] reports a violation of the unique constraint on
 * `user_id` (one consumption per user) as a `DuplicationException` through `ConstraintMapping.REGISTRY`.
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
     * Retrieves and locks the consumption of the user with the given id. With `MANDATORY`, a call outside a
     * transaction throws an `IllegalTransactionStateException`, because there the lock would end with the query.
     *
     * @throws NotFoundException if no user has the id
     */
    @Transactional(propagation = Propagation.MANDATORY)
    override fun getByUserIdForUpdate(userId: UUID): CoffeeConsumption =
        toDomainOrThrow(repository.findForUpdateByUserId(userId), userId)

    /**
     * Maps the found consumption, or reports an unknown user when none was found, because every user has exactly
     * one consumption.
     *
     * @throws NotFoundException if [entity] is null
     */
    private fun toDomainOrThrow(
        entity: CoffeeConsumptionEntity?,
        userId: UUID
    ): CoffeeConsumption = entity?.let { mapper.fromEntity(it) } ?: throw NotFoundException(User::class.java, userId)
}
