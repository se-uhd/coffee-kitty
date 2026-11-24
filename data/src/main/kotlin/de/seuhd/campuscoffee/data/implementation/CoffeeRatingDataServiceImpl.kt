package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.mapper.CoffeeRatingEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CoffeeRatingEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CoffeeRatingRepository
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import de.seuhd.campuscoffee.domain.ports.data.CoffeeRatingDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Data adapter that implements the [CoffeeRatingDataService] port on the table `coffee_ratings`. Business rules, such
 * as when a user may rate a bean, belong in a domain service. Coffee Kitty has no domain service for the ratings yet.
 * [upsert] throws a `NotFoundException` for a rating that references a user or a bean that does not exist (see
 * [CrudDataServiceImpl]).
 */
@Service
class CoffeeRatingDataServiceImpl(
    repository: CoffeeRatingRepository,
    entityMapper: CoffeeRatingEntityMapper
) : CrudDataServiceImpl<CoffeeRating, CoffeeRatingEntity, CoffeeRatingRepository, UUID>(
        repository,
        entityMapper,
        CoffeeRating::class.java
    ),
    CoffeeRatingDataService {
    override fun getAllByUserId(userId: UUID): List<CoffeeRating> =
        repository.findAllByUserIdOrderByCreatedAtDesc(userId).map { mapper.fromEntity(it) }

    override fun getAllByBeanId(beanId: UUID): List<CoffeeRating> =
        repository.findAllByBeanIdOrderByCreatedAtAsc(beanId).map { mapper.fromEntity(it) }
}
