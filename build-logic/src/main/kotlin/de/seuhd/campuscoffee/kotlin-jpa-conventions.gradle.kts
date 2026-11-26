package de.seuhd.campuscoffee

// External: https://kotlinlang.org/docs/kotlin-jpa-plugin.html Hibernate needs a no-arg constructor to instantiate a
// JPA entity, and an open class with open members to create a lazy proxy.
plugins {
    kotlin("plugin.jpa")
}
