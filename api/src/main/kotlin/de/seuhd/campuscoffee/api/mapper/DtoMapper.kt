package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.Dto
import de.seuhd.campuscoffee.domain.model.DomainModel

/**
 * Generic mapper that converts between a domain object and its DTO in both directions. With separate DTOs, the domain
 * models do not need API annotations, such as `@NotNull` or `@Schema`. A mapper for one direction, such as
 * [ConsumptionDtoMapper], does not extend [DtoMapper] and names its method `toDto`.
 *
 * @param DOMAIN the type of the domain model
 * @param DTO    the type of the DTO
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
