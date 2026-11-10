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
            // Fails when an imported production or test class belongs to no layer. A new top-level package then fails
            // the test until it is added to a layer.
            .ensureAllClassesAreContainedInArchitecture()
            .check(classes)
    }

    @Test
    fun `the production packages are free of cycles`() {
        // Fails on a cycle between production packages, inside a module or across modules. Each package is a slice of
        // its own. With one slice per module, the rule would miss a cycle inside a module, such as a cycle between
        // data.implementations and data.mapper. Test classes are left out, because the rule concerns the structure of
        // the production code. RootInclusiveSliceAssignment keeps the classes of the root package, such as
        // Application and FixtureStartupLoader, in the check.
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
        // another domain service implementation. @Configuration classes are exempt, because they compose the beans.
        // The rule checks dependencies on the concrete *Impl classes of this project only. It ignores Kotlin runtime
        // classes such as kotlin.jvm.internal.FunctionReferenceImpl, which is the supertype of every class that Kotlin
        // generates for a function reference. It also ignores the abstract CrudDataServiceImpl, which every
        // *DataServiceImpl extends. Classes nested in an *Impl class are not checked. Kotlin generates such a nested
        // class for a reference to a method of the *Impl class, and the nested class depends on the *Impl class.
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
        // The layered rule lets api and application use every class of the domain module. Without this rule, a
        // controller or the fixture loader could inject a data port and bypass the checks in the domain services,
        // such as the checks of the login name and the email address of a new user.
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
        // The internal ports, such as DataResetService, are for the fixture loader and the test setup. TestFixtures
        // holds the fixture users. No class in the api layer may use the internal ports or TestFixtures. The fixture
        // loader may use both, because it is in the application layer, which this rule does not cover.
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

    @Test
    fun `api controllers are declared as RestControllers`() {
        // Spring MVC treats the return value of a handler method in a plain @Controller as a view name, unless the
        // method has @ResponseBody. @RestController is annotated with @Controller but passes the rule, because
        // beAnnotatedWith checks only direct annotations.
        noClasses()
            .that()
            .resideInAPackage("de.seuhd.campuscoffee.api.controller..")
            .should()
            .beAnnotatedWith(Controller::class.java)
            .because("a plain @Controller resolves a handler result without @ResponseBody as a view name")
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
     * `de.seuhd.campuscoffee` goes to the slice `(root)`, which keeps the root package in the cycle check.
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
