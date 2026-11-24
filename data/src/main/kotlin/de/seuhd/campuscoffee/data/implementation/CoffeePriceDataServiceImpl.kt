package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.mapper.CoffeePriceEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CoffeePriceEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CoffeePriceRepository
import de.seuhd.campuscoffee.domain.model.CoffeePrice
import de.seuhd.campuscoffee.domain.ports.data.CoffeePriceDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * The data adapter that implements the [CoffeePriceDataService] port on the table `coffee_prices`, which holds at most
 * one row. Business rules, such as the check that only an admin changes the price, belong in a domain service. Coffee
 * Kitty has no domain service for the price yet. [upsert] throws a `DuplicationException` for a second price without an
 * id (see [CrudDataServiceImpl]).
 */
@Service
class CoffeePriceDataServiceImpl(
    repository: CoffeePriceRepository,
    entityMapper: CoffeePriceEntityMapper
) : CrudDataServiceImpl<CoffeePrice, CoffeePriceEntity, CoffeePriceRepository, UUID>(
        repository,
        entityMapper,
        CoffeePrice::class.java
    ),
    CoffeePriceDataService {
    override fun findCurrent(): CoffeePrice? = repository.findAll().firstOrNull()?.let { mapper.fromEntity(it) }
}
