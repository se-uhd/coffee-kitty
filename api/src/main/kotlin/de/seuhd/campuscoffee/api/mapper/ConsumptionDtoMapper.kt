package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper from a [CoffeeConsumption] to a [ConsumptionDto]. The mapper converts only in this direction. The
 * request bodies `ConsumptionDeltaDto` and `ConsumptionCorrectionDto` need no mapper, because the controller passes
 * their values to the domain.
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
