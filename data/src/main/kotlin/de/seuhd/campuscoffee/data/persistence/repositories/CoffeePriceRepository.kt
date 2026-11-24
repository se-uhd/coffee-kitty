package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.CoffeePriceEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for [CoffeePriceEntity]. The table holds at most one row, and the inherited `findAll`
 * returns a list with the stored price or an empty list.
 */
interface CoffeePriceRepository : JpaRepository<CoffeePriceEntity, UUID>
