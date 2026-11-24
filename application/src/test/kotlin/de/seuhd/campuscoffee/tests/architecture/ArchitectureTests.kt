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

/**
 * ArchUnit tests that check that the app follows the ports-and-adapters pattern.
 */
class ArchitectureTests {
    @Test
    fun `each layer depends only on its permitted layers`() {
        val classes =
            ClassFileImporter()
                .importPackages("de.seuhd.campuscoffee") // includes every subpackage

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
            // Fails when a production or test class belongs to no layer, for example a class in a new top-level
            // package. A developer must then add the package to a layer.
            .ensureAllClassesAreContainedInArchitecture()
            .check(classes)
    }

    @Test
    fun `the production packages have no cycles`() {
        // Fails on a cycle between production packages, inside a module or across modules. Each package is a slice of
        // its own. With one slice per module, the rule would miss a cycle inside a module, such as a cycle between
        // data.implementations and data.mapper. The rule leaves out the test classes, because it checks the structure
        // of the production code. RootInclusiveSliceAssignment includes the classes of the root package, such as
        // Application and FixtureStartupLoader.
        slices()
            .assignedFrom(RootInclusiveSliceAssignment)
            .should()
            .beFreeOfCycles()
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `production code depends on ports, never on Impl types`() {
        // No production class may depend on a concrete *Impl class instead of the port that the *Impl class
        // implements. The rule covers *Impl classes too, and a domain service implementation therefore cannot inject
        // another domain service implementation. The rule exempts @Configuration classes, because they compose the
        // beans. It also exempts a class nested in an *Impl class. Kotlin generates such a class for a reference to a
        // method of the *Impl class, and the generated class depends on the *Impl class.
        //
        // The rule checks only dependencies on the concrete *Impl classes of this project. It ignores Kotlin runtime
        // classes such as kotlin.jvm.internal.FunctionReferenceImpl, which is the supertype of every class that Kotlin
        // generates for a function reference. It also ignores the abstract CrudDataServiceImpl, which every
        // *DataServiceImpl extends.
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
    fun `api and application depend on neither the data ports nor the implementation classes of the domain`() {
        // The layered rule lets api and application use every class of the domain module. Without this rule, a
        // controller or the fixture loader could inject a data port. It would then bypass the checks in the domain
        // services, such as the checks of the login name and the email address of a new user.
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
    fun `no api class depends on the internal ports or on TestFixtures`() {
        // The internal ports, such as DataResetService, are for the fixture loader, the bootstrap admin loader, and the
        // test setup. TestFixtures holds the fixture users. The two loaders may use the internal ports and
        // TestFixtures, because they are in the application layer, which this rule does not cover.
        noClasses()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("de.seuhd.campuscoffee.domain.ports.internal..", "de.seuhd.campuscoffee.domain.tests..")
            .check(PRODUCTION_CLASSES)
    }

    @Test
    fun `the port packages contain only interfaces with names that end in Service or DataService`() {
        // Every port is an interface named *Service, and every data port is named *DataService. A concrete class
        // does not belong in ports/.
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
        // The production packages of the application module: the root package with Application and
        // FixtureStartupLoader, and `configuration` with FixturesProperties. The controllers and their configuration
        // are in the api layer.
        val APPLICATION_PRODUCTION_PACKAGES = arrayOf("de.seuhd.campuscoffee", "de.seuhd.campuscoffee.configuration..")

        // The application layer, which no other layer may access, consists of these production packages and the
        // test classes under `tests`.
        val APPLICATION_PACKAGES = APPLICATION_PRODUCTION_PACKAGES + "de.seuhd.campuscoffee.tests.."

        // The production classes of every module. The rules share one import, which runs on first use.
        val PRODUCTION_CLASSES: JavaClasses by lazy {
            ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("de.seuhd.campuscoffee")
        }
    }

    /**
     * Assigns each class to a slice named after its package relative to `de.seuhd.campuscoffee`. A class directly in
     * `de.seuhd.campuscoffee` goes to the slice `(root)`, so that the cycle check includes the root package.
     * `slices().matching("de.seuhd.campuscoffee.(**)")` would leave the root package out. A class outside
     * `de.seuhd.campuscoffee` belongs to no slice.
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
