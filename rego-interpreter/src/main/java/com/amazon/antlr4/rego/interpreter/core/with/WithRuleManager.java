// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.with;

import java.util.Collections;
import java.util.Set;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.RuleManager;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;

public class WithRuleManager extends RuleManager {

    private final WithContext externalContext;
    private WithContext internalContext;

    public WithRuleManager(WithContext externalContext) {
        this.externalContext = externalContext;
        WithJsonResolver jsonResolver = new WithJsonResolver(externalContext);
        putRule(jsonResolver.getRule("input"));
        putRule(jsonResolver.getRule("data"));
    }

    public WithRuleManager withInternalContext(WithContext internalContext) {
        this.internalContext = internalContext;
        return this;
    }

    public JsonValue getRuleValue(String ruleName) {
        Set<Rule> rules = super.searchRuleStackRecursively(ruleName);
        if (rules.size() == 0) {
            return recomputeRule(ruleName);
        }
        return super.getRuleValue(ruleName, rules);
    }

    public Set<Rule> searchRuleStackRecursively(String ruleName) {
        Set<Rule> rules = super.searchRuleStackRecursively(ruleName);
        if (rules.size() == 0) {
            rules = externalContext.ruleManager.searchRuleStackRecursively(ruleName);
        }
        return rules;
    }

    private JsonValue recomputeRule(String ruleName) {
        Set<Rule> rules = externalContext.ruleManager.searchRuleStackRecursively(ruleName);
        if (rules.size() == 0) {
            throw new RegoProcessorException.BadReferenceException("Unknown rule: " + ruleName);
        }
        Rule externalRule = rules.iterator().next();
        Rule internalRule = externalRule.copy();
        putRule(internalRule);
        internalContext.visitor.visit(internalRule.ruleContext);
        return super.getRuleValue(ruleName, Collections.singleton(internalRule));
    }
}
