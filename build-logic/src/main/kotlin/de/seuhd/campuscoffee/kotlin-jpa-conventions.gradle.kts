package de.seuhd.campuscoffee

// The Kotlin JPA plugin generates a no-arg constructor for the @Entity, @Embeddable, and @MappedSuperclass classes
// and, through its all-open `jpa` preset, opens them. Hibernate needs the constructor to instantiate an entity and the
// open class and members to create a lazy proxy. Applied only to the data module.
plugins {
    kotlin("plugin.jpa")
}
