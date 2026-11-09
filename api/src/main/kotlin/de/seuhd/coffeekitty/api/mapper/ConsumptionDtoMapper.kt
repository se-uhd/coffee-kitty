package de.seuhd.coffeekitty.api.mapper

import de.seuhd.coffeekitty.api.dtos.ConsumptionDto
import de.seuhd.coffeekitty.domain.model.CoffeeConsumption
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper from a [CoffeeConsumption] domain object to its response DTO, flattening the user to their login
 * name. The request that adds a coffee carries only a delta (`ConsumptionDeltaDto`), which the controller passes to
 * the domain as a number, so the mapper has no reverse direction and does not extend [DtoMapper].
 */
@Mapper(componentModel = "spring")
interface ConsumptionDtoMapper {
    /**
     * Maps a consumption to its response DTO.
     *
     * @param consumption the consumption to map
     */
    @Mapping(target = "loginName", source = "user.loginName")
    fun toDto(consumption: CoffeeConsumption): ConsumptionDto
}
