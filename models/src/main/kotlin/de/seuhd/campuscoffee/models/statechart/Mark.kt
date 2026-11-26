package de.seuhd.campuscoffee.models.statechart

/** Whether the model states behind a chart state break a rule. */
enum class Mark {
    NONE,
    BREAKS,
    MAY_BREAK
}
