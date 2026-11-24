package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.mapper.CountCorrectionEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.CountCorrectionEntity
import de.seuhd.campuscoffee.data.persistence.repositories.CountCorrectionRepository
import de.seuhd.campuscoffee.domain.model.CountCorrection
import de.seuhd.campuscoffee.domain.ports.data.CountCorrectionDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Data adapter that implements the [CountCorrectionDataService] port on the table `count_corrections`. The business
 * rules, such as the check that only an admin corrects a coffee count, are in the domain services. The port offers only
 * [insert] and [getAllByUserId], because a stored correction never changes. The class still extends
 * [CrudDataServiceImpl], whose `upsert` stores the new row for [insert].
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
    /**
     * Stores [correction] under a new id. The method runs in a transaction, in which it loads the user and the admin of
     * the correction and inserts the row.
     *
     * @throws IllegalArgumentException if [correction] has an id, because `upsert` would then update the stored row
     */
    @Transactional
    override fun insert(correction: CountCorrection): CountCorrection {
        require(correction.id == null) { "A stored correction cannot change." }
        return upsert(correction)
    }

    override fun getAllByUserId(userId: UUID): List<CountCorrection> =
        repository.findAllByUserIdOrderByCreatedAtDesc(userId).map { mapper.fromEntity(it) }
}
