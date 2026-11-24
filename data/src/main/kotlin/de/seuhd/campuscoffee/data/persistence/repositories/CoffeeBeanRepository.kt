package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeBeanEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

/**
 * Spring Data JPA repository for [CoffeeBeanEntity]. Spring Data implements this interface at runtime and runs the
 * query of [findActiveByName].
 */
interface CoffeeBeanRepository : JpaRepository<CoffeeBeanEntity, UUID> {
    /**
     * Returns the live bean with [name], ignoring case, or null. The partial unique index
     * [CoffeeBeanEntity.NAME_UNIQUE_CONSTRAINT] allows at most one such row.
     *
     * @param name the name to look up
     */
    @Query("select b from CoffeeBeanEntity b where lower(b.name) = lower(:name) and b.mergedIntoId is null")
    fun findActiveByName(
        @Param("name") name: String
    ): CoffeeBeanEntity?
}
