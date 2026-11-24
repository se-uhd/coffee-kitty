package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.CountCorrectionDto
import de.seuhd.campuscoffee.domain.model.CountCorrection
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * MapStruct mapper from a [CountCorrection] to a [CountCorrectionDto]. The mapper converts only in this direction (see
 * [ConsumptionDtoMapper]).
 */
@Mapper(componentModel = "spring")
interface CountCorrectionDtoMapper {
    /**
     * Converts [correction] to a [CountCorrectionDto]. The `@Mapping` copies the login name from the admin of the
     * correction.
     *
     * @param correction the correction to convert
     */
    @Mapping(target = "adminLoginName", source = "admin.loginName")
    fun toDto(correction: CountCorrection): CountCorrectionDto
}
