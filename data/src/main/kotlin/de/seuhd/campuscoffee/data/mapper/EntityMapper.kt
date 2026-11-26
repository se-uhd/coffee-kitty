package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.Entity
import de.seuhd.campuscoffee.domain.model.DomainModel
import org.mapstruct.MappingTarget

/**
 * Converts between domain objects and JPA entities. Only the JPA entities carry JPA annotations, and the domain module
 * does not depend on JPA.
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
     * Copies the values of [source] into the existing [target]. Each subinterface overrides this method to ignore `id`,
     * `createdAt`, and `updatedAt` (see [UserEntityMapper]), because without the override, the update copies the id and
     * the timestamps from the domain object.
     *
     * @param source the domain object with the new values
     * @param target the JPA entity to update in place
     */
    fun updateEntity(
        source: DOMAIN,
        @MappingTarget target: ENTITY
    )
}
