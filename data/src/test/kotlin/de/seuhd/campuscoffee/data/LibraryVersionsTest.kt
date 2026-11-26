package de.seuhd.campuscoffee.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * The tests of the versions of the database libraries. Spring Boot 4.1.2 manages versions with security advisories,
 * and `gradle.properties` overrides them with the versions that fix the advisories.
 */
class LibraryVersionsTest {
    @ParameterizedTest
    @CsvSource("org.postgresql.Driver, 42.7.15", "org.hibernate.Version, 7.4.17")
    fun `a database library has the version that fixes its advisories or a newer one`(
        className: String,
        fixedVersion: String
    ) {
        val version = Class.forName(className).`package`.implementationVersion

        val comparison =
            version
                .numbers()
                .zip(fixedVersion.numbers())
                .map { (actual, fixed) -> actual.compareTo(fixed) }
                .firstOrNull { it != 0 } ?: 0
        assertThat(comparison).`as`("the version $version of $className").isNotNegative()
    }

    private fun String.numbers(): List<Int> = split(".").mapNotNull { it.toIntOrNull() }
}
