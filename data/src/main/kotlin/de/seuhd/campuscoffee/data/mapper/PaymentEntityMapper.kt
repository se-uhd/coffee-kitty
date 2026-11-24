package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.PaymentEntity
import de.seuhd.campuscoffee.domain.model.Payment
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget

/**
 * The MapStruct mapper between [Payment] domain objects and [PaymentEntity] JPA entities. [toEntity] and [updateEntity]
 * set [PaymentEntity.user] to the stored user, which [EntityReferenceResolver] loads by the id of [Payment.user], or to
 * null for a kitty adjustment. Saving a payment then leaves the user row unchanged. [fromEntity] maps the user with all
 * fields through [UserEntityMapper]. [updateEntity] does not copy the id or the timestamps (see
 * [EntityMapper.updateEntity]).
 */
@Mapper(componentModel = "spring", uses = [UserEntityMapper::class, EntityReferenceResolver::class])
interface PaymentEntityMapper : EntityMapper<Payment, PaymentEntity> {
    @Mapping(target = "user", source = "user.id")
    override fun toEntity(source: Payment): PaymentEntity

    @Mapping(target = "user", source = "user.id")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    override fun updateEntity(
        source: Payment,
        @MappingTarget target: PaymentEntity
    )
}
