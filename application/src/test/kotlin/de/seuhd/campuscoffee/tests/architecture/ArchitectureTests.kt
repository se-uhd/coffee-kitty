package de.seuhd.campuscoffee.tests.architecture

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
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

        // the application module's own wiring: the root package (the Spring Boot app and the fixture loader) and
        // the fixture @ConfigurationProperties under `configuration`, plus the test sources. All of it is the
        // application layer, which no other layer may access. The web adapters and their configuration are in the
        // `api` layer.
        val applicationPackages =
            arrayOf(
                "de.seuhd.campuscoffee",
                "de.seuhd.campuscoffee.configuration..",
                "de.seuhd.campuscoffee.tests.."
            )

        layeredArchitecture()
            .consideringAllDependencies()
            .layer("api")
            .definedBy("de.seuhd.campuscoffee.api..")
            .layer("domain")
            .definedBy("de.seuhd.campuscoffee.domain..")
            .layer("data")
            .definedBy("de.seuhd.campuscoffee.data..")
            .layer("application")
            .definedBy(*applicationPackages)
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
        // No package may depend (even transitively) back on a package that depends on it, across every
        // module. Each distinct package path is its own slice, so a legitimate one-way edge (e.g.,
        // data.implementations -> data.persistence.repositories) is not mistaken for a cycle. Test sources are
        // excluded, because the rule is about the production structure.
        val productionClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("de.seuhd.campuscoffee")

        // A `matching("...(**)")` pattern cannot capture a class that sits directly in the root package
        // (there is no sub-package segment to match), so the application main class and the fixture loader
        // would be silently excluded from cycle analysis. Assign slices explicitly instead, giving root-package
        // classes their own "(root)" slice so they participate too.
        slices()
            .assignedFrom(RootInclusiveSliceAssignment)
            .should()
            .beFreeOfCycles()
            .check(productionClasses)
    }

    @Test
    fun `production code depends on ports, never on Impl types`() {
        val productionClasses =
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("de.seuhd.campuscoffee")

        // Everything depends on the port, not the implementation. @Configuration classes are exempt because they
        // exist to compose the beans. The target is restricted to our own *Impl classes so that a Kotlin runtime
        // *Impl (e.g., a lambda's FunctionReferenceImpl supertype) is not mistaken for a layering violation. The
        // source set also excludes *Impl-named classes (haveSimpleNameNotEndingWith("Impl")), because each
        // *DataServiceImpl extends the base CrudDataServiceImpl, an inheritance edge that would otherwise trip the
        // rule. The trade-off is that one *Impl depending on another is not checked, which is acceptable because
        // the adapters are the leaves of the dependency graph and none depends on another today.
        val isOwnImplementation =
            object : DescribedPredicate<JavaClass>("a campus-coffee class whose simple name ends with 'Impl'") {
                override fun test(javaClass: JavaClass): Boolean =
                    javaClass.simpleName.endsWith("Impl") && javaClass.packageName.startsWith("de.seuhd.campuscoffee")
            }

        noClasses()
            .that()
            .haveSimpleNameNotEndingWith("Impl")
            .and()
            .areNotAnnotatedWith(Configuration::class.java)
            .should()
            .dependOnClassesThat(isOwnImplementation)
            .check(productionClasses)
    }

    @Test
    fun `api and application reach the domain without its data ports and implementation classes`() {
        // The layered rule works per module, so on its own it lets a controller or the fixture loader inject a data
        // port and skip the domain's rules, such as the checks on a new user's login name and email address.
        noClasses()
            .that()
            .resideInAnyPackage(
                "de.seuhd.campuscoffee.api..",
                "de.seuhd.campuscoffee",
                "de.seuhd.campuscoffee.configuration.."
            ).should()
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
        // A concrete class is not a port (it belongs in domain.model), and every port is a *Service. A data port
        // is a *DataService.
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
