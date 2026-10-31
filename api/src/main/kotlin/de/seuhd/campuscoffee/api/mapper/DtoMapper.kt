package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.Dto
import de.seuhd.campuscoffee.domain.model.DomainModel

/**
 * Generic mapper between a domain model and its DTO, in both directions, so that the domain model needs no API
 * annotations. A mapper that maps only from the domain does not extend it and names its method `toDto`, as
 * [ConsumptionDtoMapper] does.
 *
 * @param DOMAIN the domain model type
 * @param DTO    the data transfer object type
 */
interface DtoMapper<DOMAIN : DomainModel<*>, DTO : Dto<*>> {
    /**
     * Converts a domain object to its DTO.
     *
     * @param source the domain object to convert
     */
    fun fromDomain(source: DOMAIN): DTO

    /**
     * Converts a DTO to its domain object.
     *
     * @param source the DTO to convert
     */
    fun toDomain(source: DTO): DOMAIN
}
