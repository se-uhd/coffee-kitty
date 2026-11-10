package de.seuhd.campuscoffee.data.persistence

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

/**
 * Checks that [ConstraintMapping.REGISTRY] lists exactly the unique constraints and unique indexes of the migrated
 * schema. The test fails when a migration adds a unique constraint without an entry in the registry. Without the
 * entry, a duplicate value for that constraint leads to a 500 instead of a 409.
 */
class ConstraintMappingRegistryIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun `the constraint registry lists each non-primary unique index of the app's tables once and nothing else`() {
        // The query skips flyway_schema_history, because it is the only table without a JPA entity
        val uniqueIndexes =
            jdbcTemplate.queryForList(
                "SELECT c.relname FROM pg_index i " +
                    "JOIN pg_class c ON c.oid = i.indexrelid " +
                    "JOIN pg_class t ON t.oid = i.indrelid " +
                    "JOIN pg_namespace n ON n.oid = t.relnamespace " +
                    "WHERE i.indisunique AND NOT i.indisprimary AND n.nspname = current_schema() " +
                    "AND t.relname <> 'flyway_schema_history'",
                String::class.java
            )

        assertThat(uniqueIndexes).isNotEmpty()
        assertThat(ConstraintMapping.REGISTRY.map { it.constraintName })
            .doesNotHaveDuplicates()
            .containsExactlyInAnyOrderElementsOf(uniqueIndexes)
    }
}
