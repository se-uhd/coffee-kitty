package de.seuhd.coffeekitty.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Registers the KDoc rule set `coffee-kitty-kdoc` with detekt. The rules are loaded
 * via the `detektPlugins(project(":detekt-rules"))` dependency and activated in `config/detekt/detekt.yml`.
 */
class KDocRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("coffee-kitty-kdoc")

    override fun instance(): RuleSet =
        RuleSet(
            ruleSetId,
            listOf(
                ::UndocumentedNonLocalClass,
                ::UndocumentedFunction,
                ::UndocumentedFunctionParameter,
                ::UndocumentedEnumConstructorProperty
            )
        )
}
