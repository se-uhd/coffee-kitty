package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.CountCorrectionEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CountCorrectionEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CountCorrectionRepository
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Data adapter that implements the [CountCorrectionDataService] port on the `count_corrections` table. The business
 * rules, such as the check that only an admin corrects a coffee count, are in the domain services. [upsert] throws a
 * `NotFoundException` for a correction that references a user who does not exist (see [CrudDataServiceImpl]).
 */
@Service
class CountCorrectionDataServiceImpl(
    repository: CountCorrectionRepository,
    entityMapper: CountCorrectionEntityMapper
) : CrudDataServiceImpl<CountCorrection, CountCorrectionEntity, CountCorrectionRepository, UUID>(
        repository,
        entityMapper,
        CountCorrection::class.java
    ),
    CountCorrectionDataService {
    override fun getAllByUserId(userId: UUID): List<CountCorrection> =
        repository.findAllByUserIdOrderByCreatedAtDesc(userId).map { mapper.fromEntity(it) }
}
