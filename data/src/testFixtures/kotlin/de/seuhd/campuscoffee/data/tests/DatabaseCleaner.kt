package de.seuhd.campuscoffee.data.tests

import javax.sql.DataSource

/**
 * Deletes the rows of every table of the app, so that each test and each start of the dev application begins with an
 * empty database. The class is in the test fixtures of the data module. The production jar does not contain it, and a
 * deployed app therefore cannot delete its data.
 *
 * [clean] reads the names of the tables from the PostgreSQL catalog and does not list them. A table that a new
 * migration creates is cleaned without a change to this class. One `TRUNCATE` statement empties all tables together,
 * so the foreign keys between them do not require an order. The table `flyway_schema_history` keeps its rows, because
 * Flyway reads from it which migrations it has applied.
 *
 * @param dataSource the data source of the database to clean
 */
class DatabaseCleaner(
    private val dataSource: DataSource
) {
    /** Deletes the rows of every table in the current schema except `flyway_schema_history`. */
    fun clean() {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                val tables = mutableListOf<String>()
                statement.executeQuery(TABLES_QUERY).use { rows ->
                    while (rows.next()) {
                        tables += rows.getString(1)
                    }
                }
                if (tables.isNotEmpty()) {
                    statement.execute("TRUNCATE TABLE ${tables.joinToString { "\"$it\"" }}")
                }
            }
        }
    }

    private companion object {
        /** The name of the table in which Flyway records the applied migrations. */
        const val FLYWAY_TABLE = "flyway_schema_history"

        /** Selects the names of the tables in the current schema except [FLYWAY_TABLE]. */
        const val TABLES_QUERY =
            "SELECT tablename FROM pg_tables WHERE schemaname = current_schema() AND tablename <> '$FLYWAY_TABLE'"
    }
}
