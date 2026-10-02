package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.CoffeeConsumptionEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CoffeeConsumptionRepository
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * The data layer's adapter for the [CoffeeConsumptionDataService] port over the `coffee_consumptions` table. The
 * business rules are in the domain services. [upsert] reports a violation of the unique constraint on `user_id` (one
 * consumption per user) as a `DuplicationException` through `ConstraintMapping.REGISTRY`.
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
    /**
     * Retrieves the consumption belonging to the user with the given id.
     *
     * @throws NotFoundException if the user has no consumption
     */
    override fun getByUserId(userId: UUID): CoffeeConsumption =
        findByFieldOrThrow({ repository.findByUserId(userId) }, "user_id", userId.toString())
}
