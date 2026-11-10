package de.seuhd.campuscoffee

// The Kotlin JPA plugin generates a no-arg constructor for each class annotated with @Entity, @Embeddable, or
// @MappedSuperclass. The Kotlin JPA plugin also applies the `jpa` preset of the all-open plugin, which opens these
// classes and their members. Hibernate needs the constructor to instantiate an entity, and the open class and members
// to create a lazy proxy. Only the data module applies this convention plugin.
plugins {
    kotlin("plugin.jpa")
}
