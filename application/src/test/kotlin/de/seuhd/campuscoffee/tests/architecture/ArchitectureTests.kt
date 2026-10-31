package de.seuhd.campuscoffee.tests.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SliceAssignment
import com.tngtech.archunit.library.dependencies.SliceIdentifier
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller

/**
 * Architecture tests that ensure that the application follows the ports-and-adapters pattern.
 */
class ArchitectureTests {
    @Test
    fun `each layer depends only on its permitted layers`() {
        val classes =
            ClassFileImporter()
                .importPackages("de.seuhd.campuscoffee") // imports all sub-packages

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
            // every imported production or test class must belong to one of the layers above, so a new top-level
            // package cannot silently escape the layer rules
            .ensureAllClassesAreContainedInArchitecture()
            .check(classes)
    }

    @Test
    fun `the production packages are free of cycles`() {
        // No package may depend (even transitively) back on a package that depends on it, across every module. Each
        // distinct package path is its own slice, so that a cycle between two packages of one module (e.g.,
        // data.implementations and data.mapper) also fails, which one slice per module would hide. Test sources are
        // excluded, because the rule is about the production structure. RootInclusiveSliceAssignment keeps the classes
        // of the root package (Application and FixtureStartupLoader) in the cycle check.
        slices()
            .assignedFrom(RootInclusiveSliceAssignment)
            .should()
            .beFreeOfCycles()
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `production code depends on ports, never on Impl types`() {
        // No production class, not even an *Impl class, may depend on a concrete *Impl class instead of its port, so
        // that a domain service implementation cannot inject another one. @Configuration classes are exempt because
        // they exist to compose the beans. The targets are the project's own concrete *Impl classes: a Kotlin runtime
        // *Impl (e.g., kotlin.jvm.internal.FunctionReferenceImpl, the supertype of a class that Kotlin generates for a
        // function reference) is not a layering violation, and the abstract CrudDataServiceImpl is the base class that
        // each *DataServiceImpl extends. The rule does not check a class nested in an *Impl, such as the class that
        // Kotlin generates for a reference to one of its methods, which depends on its outer class.
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
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `api and application reach the domain without its data ports and implementation classes`() {
        // The layered rule works per module, so on its own it lets a controller or the fixture loader inject a data
        // port and skip the domain's rules, such as the checks on a new user's login name and email address.
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
    fun `no api class reaches the internal ports and the fixtures`() {
        // The internal ports, such as the reset that deletes all data, are for the fixture loader and the test setup,
        // and the fixtures are seed data. No request handler may use either. The rule covers only the api layer,
        // which leaves the fixture loader in the application layer free to use both.
        noClasses()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("de.seuhd.campuscoffee.domain.ports.internal..", "de.seuhd.campuscoffee.domain.tests..")
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `the port packages hold only interfaces named after the port conventions`() {
        // A concrete class is not a port and stays out of ports/, and every port is a *Service. A data port is a
        // *DataService.
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

    @Test
    fun `api controllers are declared as RestControllers`() {
        // A plain @Controller resolves a handler result that has no @ResponseBody as a view name. The meta-annotated
        // @RestController passes, because beAnnotatedWith checks only the direct annotations.
        noClasses()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.api.controller..")
            .should()
            .beAnnotatedWith(Controller::class.java)
            .because("a plain @Controller resolves a handler result without @ResponseBody as a view name")
            .check(PRODUCTION_CLASSES)
    }

    private companion object {
        // The application module's own wiring: the root package (the Spring Boot app and the fixture loader) and
        // the fixture @ConfigurationProperties under `configuration`. The controllers and their configuration are in
        // the `api` layer.
        val APPLICATION_PRODUCTION_PACKAGES = arrayOf("de.seuhd.campuscoffee", "de.seuhd.campuscoffee.configuration..")

        // The application layer, which no other layer may access: the production packages plus the test sources.
        val APPLICATION_PACKAGES = APPLICATION_PRODUCTION_PACKAGES + "de.seuhd.campuscoffee.tests.."

        // every production class of every module, imported once for the rules that use it
        val PRODUCTION_CLASSES: JavaClasses by lazy {
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("de.seuhd.campuscoffee")
        }
    }

    /**
     * Slices every production class by its package path relative to `de.seuhd.campuscoffee`, assigning a
     * class that sits directly in the root package to a dedicated `(root)` slice so it is not dropped from
     * cycle analysis (an unqualified `matching("...(**)")` would ignore it).
     */
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
