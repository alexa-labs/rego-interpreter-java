// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.KeyFinder;
import com.amazon.antlr4.rego.interpreter.core.type.RegoFunction;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;
import com.amazon.antlr4.rego.interpreter.core.type.SomeRule;
import com.amazon.antlr4.rego.interpreter.core.type.UnderscoreLocation;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Provides stacks of rules managed by RuleRunner.
 *
 * <p>This class provides a loose implementation of the
 * <a href="https://en.wikipedia.org/wiki/Memento_pattern">Memento</a> design pattern.
 * It maintains state across scopes.
 * It also fulfills the roles of Originator and Caretaker.
 *
 * <p>This class also provides an inverted implementation of the
 * <a href="https://en.wikipedia.org/wiki/Observer_pattern">Observer</a> design pattern.
 * Rules can <code>get</code> other Rules, which will block till the Rule being read has finished execution.
 */
public class RuleManager {

    private SymbolTableStack<Set<Rule>> ruleStack = new SymbolTableStack<>();

    public void pushScope(List<Rule> rules) {
        ruleStack.pushScope();
        rules.forEach(r -> putRule(r));
    }

    public void popScope() {
        ruleStack.popScope();
    }

    public Rule getRule(ParserRuleContext ctx) {
        return getRule(ctx, KeyFinder.getKeyFromContext(ctx));
    }

    public Rule getRule(ParserRuleContext ctx, String ruleName) {
        Set<Rule> rules = searchRuleStackRecursively(ruleName);
        if (rules.size() == 1) {
            return rules.iterator().next();
        }
        if (rules.size() > 1 && ruleStack.getStackDepth() > 1) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                String.format("Rule %s has too many definitions", ruleName));
        }
        for (Rule rule: rules) {
            if (rule.ruleContext == ctx) {
                return rule;
            }
        }
        throw new RegoProcessorException.UndefinedRuleException("Rule is not defined: " + ruleName);
    }

    public JsonValue getRuleValue(String ruleName) {
        Set<Rule> rules = searchRuleStackRecursively(ruleName);
        return getRuleValue(ruleName, rules);
    }

    protected JsonValue getRuleValue(String ruleName, Set<Rule> rules) {
        if (rules.size() == 0) {
            return newSomeRule(ruleName);
        } else if (ruleHasNoValue(rules)) {
            throw new RegoProcessorException.UndefinedRuleException("Rule is not defined: " + ruleName);
        }
        return getMergedValues(rules);
    }

    private JsonValue newSomeRule(String ruleName) {
        SomeRule r = new SomeRule(ruleName, null);
        putRule(r);
        return r;
    }

    private boolean ruleHasNoValue(Set<Rule> rules) {
        return rules.stream().noneMatch(Rule::hasValue);
    }

    private JsonValue getMergedValues(Set<Rule> rules) {
        Set<Rule> rulesWithValues = rules.stream().filter(
            Rule::hasValue).collect(Collectors.toSet()
        );
        return rules.iterator().next().mergeValues(rulesWithValues);
    }

    public UnderscoreLocation getUnderscoreLocation() {
        Set<Rule> locationSet = ruleStack.get(UnderscoreLocation.UNDERSCORE_RULE_KEY);
        return (UnderscoreLocation) locationSet.iterator().next();
    }

    public void putRule(Rule rule) {
        ruleStack.computeIfAbsent(rule.key, k -> new HashSet<>()).add(rule);
    }

    public boolean isScopeValid() {
        return isScopeValid(false);
    }

    public boolean isScopeValid(boolean ignoreRequested) {
        for (Map.Entry<String, Set<Rule>> entry: ruleStack.getLocalTable().entrySet()) {
            Set<Rule> rules = entry.getValue();
            for (Rule rule : rules) {
                if (rule instanceof SomeRule || rule instanceof UnderscoreLocation) {
                    continue;
                }
                if (isRuleFailed(rule, ignoreRequested)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isRuleFailed(Rule rule, boolean ignoreRequested) {
        return rule.isFailed()
            || cannotIgnorePendingRule(rule, ignoreRequested)
            || unnamedRuleEvaluatedFalse(rule);
    }

    private boolean cannotIgnorePendingRule(Rule rule, boolean ignoreRequested) {
        return !ignoreRequested && !rule.isSuccessful();
    }

    private boolean unnamedRuleEvaluatedFalse(Rule rule) {
        return rule.isSuccessful() && KeyFinder.isUnnamedKey(rule.key) && rule.getValue() == JsonValue.FALSE;
    }

    public Set<Rule> searchRuleStackRecursively(String ruleName) {
        return ruleStack.searchRuleStack(ruleName, () -> Collections.emptySet());
    }

    public Map<String, Set<Rule>> getRules(boolean withFunctions) {
        return ruleStack.getGlobalTable().entrySet().stream()
        .filter(es -> withFunctions || !(es.getValue().iterator().next() instanceof RegoFunction))
        .filter(es -> es.getValue().stream().filter(ess -> ess.hasValue()).count() > 0)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue));
    }
}
