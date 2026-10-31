package de.seuhd.campuscoffee.api.dtos

import de.seuhd.campuscoffee.domain.model.Identifiable

/**
 * Base type of the DTOs that have an id, such as [UserDto]. It adds no member to [Identifiable], and
 * [de.seuhd.campuscoffee.api.mapper.DtoMapper] takes only its subtypes. A DTO without an id, such as
 * [ConsumptionDto], does not implement it.
 */
interface Dto<ID> : Identifiable<ID>
