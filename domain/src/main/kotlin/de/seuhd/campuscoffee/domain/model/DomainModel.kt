package de.seuhd.campuscoffee.domain.model

/**
 * The base type of every domain model. It adds no member to [Identifiable]. The generic interfaces `CrudDataService`
 * and `EntityMapper` declare their type parameter `DOMAIN` with the upper bound `DomainModel`. With this bound, the
 * compiler rejects any other class as the argument for `DOMAIN`.
 */
interface DomainModel<ID> : Identifiable<ID>
