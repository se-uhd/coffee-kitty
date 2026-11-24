package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.mapper.CoffeeBeanEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CoffeeBeanEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CoffeeBeanRepository
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * The data adapter that implements the [CoffeeBeanDataService] port on the table `coffee_beans`. Business rules, such
 * as trimming a name and merging two beans, belong in a domain service. Coffee Kitty has no domain service for the
 * beans yet. [upsert] throws a `DuplicationException` for a live bean with the same name as another live bean, ignoring
 * case (see [CrudDataServiceImpl]).
 */
@Service
class CoffeeBeanDataServiceImpl(
    repository: CoffeeBeanRepository,
    entityMapper: CoffeeBeanEntityMapper
) : CrudDataServiceImpl<CoffeeBean, CoffeeBeanEntity, CoffeeBeanRepository, UUID>(
        repository,
        entityMapper,
        CoffeeBean::class.java
    ),
    CoffeeBeanDataService {
    override fun findActiveByName(name: String): CoffeeBean? =
        repository.findActiveByName(name)?.let { mapper.fromEntity(it) }
}
