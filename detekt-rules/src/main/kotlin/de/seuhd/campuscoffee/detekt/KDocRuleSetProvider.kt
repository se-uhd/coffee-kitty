package de.seuhd.campuscoffee.detekt

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/**
 * Provides the KDoc rule set `campus-coffee-kdoc` to detekt, which finds this class through the service file
 * `META-INF/services/dev.detekt.api.RuleSetProvider`. kotlin-conventions adds the rules to detekt with
 * `detektPlugins(project(":detekt-rules"))`, and `config/detekt/detekt.yml` enables them.
 */
class KDocRuleSetProvider : RuleSetProvider {
    override val ruleSetId = RuleSetId("campus-coffee-kdoc")

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
