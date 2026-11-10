package de.seuhd.campuscoffee.domain.model

/**
 * Base type of every domain model. It adds no member to [Identifiable]. The generic interfaces `CrudDataService`,
 * `EntityMapper`, and `DtoMapper` declare their type parameter `DOMAIN` with the upper bound `DomainModel`. With this
 * bound, the compiler rejects a DTO as the argument for `DOMAIN`, although a DTO is also [Identifiable].
 */
interface DomainModel<ID> : Identifiable<ID>
