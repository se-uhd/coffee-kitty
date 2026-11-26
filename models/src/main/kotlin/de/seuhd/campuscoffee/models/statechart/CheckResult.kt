package de.seuhd.campuscoffee.models.statechart

/**
 * The result of checking a model with TLC.
 *
 * @property graph the complete state graph
 * @property rules the INVARIANTs of the configuration
 * @property broken the rules that break, in the order of the configuration, and then a deadlock
 */
class CheckResult(
    val graph: StateGraph,
    val rules: List<String>,
    val broken: List<BrokenRule>
)
