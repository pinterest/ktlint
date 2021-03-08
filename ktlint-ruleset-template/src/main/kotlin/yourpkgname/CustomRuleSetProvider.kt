package yourpkgname

import com.pinterest.ktlint.core.RuleSet
import com.pinterest.ktlint.core.RuleSetProvider

public class CustomRuleSetProvider : RuleSetProvider {

    override fun get(): RuleSet = RuleSet("custom", NoVarRule())
}
