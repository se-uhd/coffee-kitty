package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.Identifiable

/**
 * Base type of the DTOs that have an id, such as [UserDto]. The interface adds no member to [Identifiable]. The type
 * parameter `DTO` of [de.seuhd.campuscoffee.api.mapper.DtoMapper] must be a subtype of [Dto]. A DTO without an id,
 * such as [ConsumptionDto], does not implement [Dto].
 */
interface Dto<ID> : Identifiable<ID>
