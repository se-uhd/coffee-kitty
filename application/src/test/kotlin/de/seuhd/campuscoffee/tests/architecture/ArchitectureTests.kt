package de.seuhd.campuscoffee.tests.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage
import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SliceAssignment
import com.tngtech.archunit.library.dependencies.SliceIdentifier
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration

/**
 * The ArchUnit tests that check that the app follows the ports-and-adapters pattern.
 */
class ArchitectureTests {
    @Test
    fun `each layer depends only on its permitted layers`() {
        val classes =
            ClassFileImporter()
                .importPackages("de.seuhd.campuscoffee")

        layeredArchitecture()
            .consideringAllDependencies()
            .layer("api")
            .definedBy("de.seuhd.campuscoffee.api..")
            .layer("domain")
            .definedBy("de.seuhd.campuscoffee.domain..")
            .layer("data")
            .definedBy("de.seuhd.campuscoffee.data..")
            .layer("application")
            .definedBy(*APPLICATION_PACKAGES)
            .whereLayer("api")
            .mayOnlyBeAccessedByLayers("application")
            .whereLayer("domain")
            .mayOnlyBeAccessedByLayers("api", "data", "application")
            .whereLayer("data")
            .mayOnlyBeAccessedByLayers("application")
            .whereLayer("application")
            .mayNotBeAccessedByAnyLayer()
            .ensureAllClassesAreContainedInArchitecture()
            .check(classes)
    }

    @Test
    fun `the production packages have no cycles`() {
        slices()
            .assignedFrom(RootInclusiveSliceAssignment)
            .should()
            .beFreeOfCycles()
            .because(
                "each package is a slice of its own, so that a cycle between two packages of one module, such as " +
                    "data.implementation and data.mapper, fails too"
            ).check(PRODUCTION_CLASSES)
    }

    @Test
    fun `production code depends on ports, never on Impl types`() {
        val isConcreteOwnImplementation =
            object : DescribedPredicate<JavaClass>("a concrete campus-coffee class named *Impl") {
                override fun test(javaClass: JavaClass): Boolean =
                    javaClass.simpleName.endsWith("Impl") &&
                        JavaModifier.ABSTRACT !in javaClass.modifiers &&
                        javaClass.packageName.startsWith("de.seuhd.campuscoffee")
            }
        val isNestedInAnImplementation =
            object : DescribedPredicate<JavaClass>("nested in a class named *Impl") {
                override fun test(javaClass: JavaClass): Boolean =
                    javaClass.name.contains('$') && javaClass.name.substringBefore('$').endsWith("Impl")
            }

        noClasses()
            .that()
            .areNotAnnotatedWith(Configuration::class.java)
            .and(DescribedPredicate.not(isNestedInAnImplementation))
            .should()
            .dependOnClassesThat(isConcreteOwnImplementation)
            .because(
                "code depends on the port that an Impl class implements, @Configuration classes compose the beans, " +
                    "and Kotlin generates the classes nested in an Impl class for its method references"
            ).check(PRODUCTION_CLASSES)
    }

    @Test
    fun `api and application depend on neither the data ports nor the implementation classes of the domain`() {
        // ADR-001 only application wires the modules together
        noClasses()
            .that()
            .resideInAnyPackage("de.seuhd.campuscoffee.api..", *APPLICATION_PRODUCTION_PACKAGES)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "de.seuhd.campuscoffee.domain.ports.data..",
                "de.seuhd.campuscoffee.domain.implementation.."
            ).check(PRODUCTION_CLASSES)
    }

    @Test
    fun `the data module uses only the models, the exceptions, and the data ports of the domain`() {
        val allowedDomainPackages =
            resideInAnyPackage(
                "de.seuhd.campuscoffee.domain.model..",
                "de.seuhd.campuscoffee.domain.exceptions..",
                "de.seuhd.campuscoffee.domain.ports.data.."
            )
        classes()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.data..")
            .should()
            .onlyDependOnClassesThat(
                resideOutsideOfPackage("de.seuhd.campuscoffee.domain..").or(allowedDomainPackages)
            ).because(
                "a data adapter implements a data port and calls no API port, no internal port, and no class of " +
                    "domain/.../implementation/, and a new package of the domain needs an entry here"
            ).check(PRODUCTION_CLASSES)
    }

    @Test
    fun `no api class depends on the internal ports`() {
        // ADR-003 the api module passes the acting user to an API port
        noClasses()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("de.seuhd.campuscoffee.domain.ports.internal..")
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `a controller function that reads the acting user is not an open endpoint`() {
        // ADR-011 an open endpoint has no acting user
        val readsTheActingUser =
            object : DescribedPredicate<JavaMethod>("call CurrentUserProvider.currentUser") {
                override fun test(method: JavaMethod): Boolean =
                    method.methodCallsFromSelf.any {
                        it.targetOwner.name == "de.seuhd.campuscoffee.api.security.CurrentUserProvider" &&
                            it.name == "currentUser"
                    }
            }
        methods()
            .that()
            .areDeclaredInClassesThat()
            .resideInAPackage("de.seuhd.campuscoffee.api.controller..")
            .and(readsTheActingUser)
            .should()
            .notBeAnnotatedWith("de.seuhd.campuscoffee.api.security.OpenEndpoint")
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `the system and acceptance tests use no class of the api module`() {
        // ADR-007 the tests pin the JSON field names
        noClasses()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.tests..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("de.seuhd.campuscoffee.api..")
            .check(ClassFileImporter().importPackages("de.seuhd.campuscoffee"))
    }

    @Test
    fun `the production classes contain no test fixtures`() {
        // ADR-005 the test fixtures are outside the production code
        noClasses()
            .should()
            .resideInAnyPackage("de.seuhd.campuscoffee.domain.tests..", "de.seuhd.campuscoffee.data.tests..")
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `the port packages contain only interfaces with names that end in Service or DataService`() {
        classes()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.domain.ports..")
            .and()
            .areTopLevelClasses()
            .should()
            .beInterfaces()
            .andShould()
            .haveSimpleNameEndingWith("Service")
            .check(PRODUCTION_CLASSES)
        classes()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.domain.ports.data..")
            .and()
            .areTopLevelClasses()
            .should()
            .haveSimpleNameEndingWith("DataService")
            .check(PRODUCTION_CLASSES)
    }

    private companion object {
        val APPLICATION_PRODUCTION_PACKAGES = arrayOf("de.seuhd.campuscoffee", "de.seuhd.campuscoffee.configuration..")

        val APPLICATION_PACKAGES =
            APPLICATION_PRODUCTION_PACKAGES + "de.seuhd.campuscoffee.tests.." + "de.seuhd.campuscoffee.dev.."

        val PRODUCTION_CLASSES: JavaClasses by lazy {
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption { location -> !location.contains("test-fixtures") }
                .importPackages("de.seuhd.campuscoffee")
        }
    }

    // External: https://www.archunit.org/userguide/html/000_Index.html#_slices matching("de.seuhd.campuscoffee.(**)")
    // assigns no slice to a class of the root package, so the cycle check would leave such a class out.
    private object RootInclusiveSliceAssignment : SliceAssignment {
        private const val BASE_PACKAGE = "de.seuhd.campuscoffee"

        override fun getIdentifierOf(javaClass: JavaClass): SliceIdentifier {
            val packageName = javaClass.packageName
            if (packageName != BASE_PACKAGE && !packageName.startsWith("$BASE_PACKAGE.")) {
                return SliceIdentifier.ignore()
            }
            val relative = packageName.removePrefix(BASE_PACKAGE).removePrefix(".")
            return SliceIdentifier.of(relative.ifEmpty { "(root)" })
        }

        override fun getDescription(): String = "campus-coffee package slices, including the root package"
    }
}
