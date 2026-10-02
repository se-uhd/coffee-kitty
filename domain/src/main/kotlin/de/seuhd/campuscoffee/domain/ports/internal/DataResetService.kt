package de.seuhd.campuscoffee.domain.ports.internal

/**
 * Internal port for deleting all data, in an order that the foreign keys allow. The fixture loader's reset on
 * startup and the test setups call it. The architecture tests keep every api class off this port, so that no
 * endpoint can offer it.
 */
interface DataResetService {
    /** Deletes every coffee consumption and then every user. */
    fun clearAll()
}
