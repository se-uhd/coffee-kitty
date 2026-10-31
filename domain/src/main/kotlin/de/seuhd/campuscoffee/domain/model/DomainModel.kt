package de.seuhd.campuscoffee.domain.model

/**
 * Base type of every domain model. It adds no member to [Identifiable]. The generic data ports and mappers bound their
 * `DOMAIN` type parameter by it (e.g., `CrudDataService<DOMAIN : DomainModel<ID>, ID>`), so that a DTO, which is also
 * [Identifiable], cannot take its place.
 */
interface DomainModel<ID> : Identifiable<ID>
