package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper from a [CoffeeConsumption] to a [ConsumptionDto]. The mapper converts in one direction and does
 * not extend [DtoMapper]. The opposite direction is not needed, because the controller passes the delta from the
 * request body (`ConsumptionDeltaDto`) to the domain as a number.
 */
@Mapper(componentModel = "spring")
interface ConsumptionDtoMapper {
    /**
     * Converts [consumption] to a [ConsumptionDto]. The `@Mapping` copies the login name from the user of the
     * consumption.
     *
     * @param consumption the consumption to convert
     */
    @Mapping(target = "loginName", source = "user.loginName")
    fun toDto(consumption: CoffeeConsumption): ConsumptionDto
}
