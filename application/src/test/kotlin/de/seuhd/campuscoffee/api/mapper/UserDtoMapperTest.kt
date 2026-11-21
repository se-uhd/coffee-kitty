package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.UserDto
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

/**
 * Unit tests for [UserDtoMapper] with `UserDtoMapperImpl`, the implementation that MapStruct generates. The tests
 * create the mapper with its constructor and need no Spring context. [user] and [dto] hold the same values, so that
 * a test can compare a whole mapped object with one of them.
 */
class UserDtoMapperTest {
    private val mapper: UserDtoMapper = UserDtoMapperImpl()

    private val createdAt: LocalDateTime = LocalDateTime.of(2026, 11, 16, 10, 0, 0)

    private val user =
        User(
            id = UUID(0L, 1L),
            createdAt = createdAt,
            updatedAt = createdAt.plusMinutes(1),
            loginName = "jane_doe",
            emailAddress = "jane.doe@se.de",
            firstName = "Jane",
            lastName = "Doe",
            role = Role.ADMIN
        )

    private val dto =
        UserDto(
            id = UUID(0L, 1L),
            createdAt = createdAt,
            updatedAt = createdAt.plusMinutes(1),
            loginName = "jane_doe",
            emailAddress = "jane.doe@se.de",
            firstName = "Jane",
            lastName = "Doe",
            role = Role.ADMIN
        )

    @Test
    fun `fromDomain copies every property of the user to the DTO`() {
        assertThat(mapper.fromDomain(user)).isEqualTo(dto)
    }

    @Test
    fun `toDomain copies every property of the DTO to the user`() {
        assertThat(mapper.toDomain(dto)).isEqualTo(user)
    }

    @Test
    fun `toDomain sets a missing role to USER`() {
        assertThat(mapper.toDomain(dto.copy(role = null)).role).isEqualTo(Role.USER)
    }

    @Test
    fun `toDomain of a DTO without an id returns a user without an id`() {
        assertThat(mapper.toDomain(dto.copy(id = null)).id).isNull()
    }
}
