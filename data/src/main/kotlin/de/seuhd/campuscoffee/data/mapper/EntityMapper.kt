package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.Entity
import de.seuhd.campuscoffee.domain.model.DomainModel
import org.mapstruct.MappingTarget

/**
 * Generic mapper between a domain model and its JPA entity, in both directions, so that the domain model needs no
 * persistence annotations.
 *
 * @param DOMAIN the domain model type
 * @param ENTITY the JPA entity type
 */
interface EntityMapper<DOMAIN : DomainModel<*>, ENTITY : Entity> {
    /**
     * Converts a JPA entity to its domain object.
     *
     * @param source the JPA entity to convert
     */
    fun fromEntity(source: ENTITY): DOMAIN

    /**
     * Converts a domain object to a new JPA entity.
     *
     * @param source the domain object to convert
     */
    fun toEntity(source: DOMAIN): ENTITY

    /**
     * Updates an existing JPA entity with the values of the domain object. A subinterface must override this method
     * and ignore `id`, `createdAt`, and `updatedAt` with `@Mapping(target = ..., ignore = true)`, as
     * [UserEntityMapper] does, because the id names the stored row and the lifecycle callbacks of [Entity] set the
     * timestamps. Without the override, the update copies them from the domain object.
     *
     * @param source the domain object holding the new values
     * @param target the JPA entity to update in place
     */
    fun updateEntity(
        source: DOMAIN,
        @MappingTarget target: ENTITY
    )
}
