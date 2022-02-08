// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.Set;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.KeyFinder;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;

import org.antlr.v4.runtime.ParserRuleContext;

import lombok.extern.log4j.Log4j2;

/**
 * This class represents a single partial/complete/unnamed rule.
 */
@Log4j2
public abstract class Rule {
    private enum RuleState {
        REQUESTED, SUCCEEDED, FAILED
    };

    /**
     * Key of the named rule. Or a UUID when no name is available.
     */
    public final String key;
    /**
     * Tree context reference for this rule.
     */
    public final ParserRuleContext ruleContext;

    private RuleState requestState = RuleState.REQUESTED;

    public Rule copy() {
        throw new RuntimeException("This rule type cannot be copied: " + this.getClass());
    }

    protected Rule(String key, ParserRuleContext context) {
        this.key = key;
        this.ruleContext = context;
    }

    public void setValue(JsonValue... values) {
        logRuleSet(values);
        requestState = RuleState.SUCCEEDED;
    }

    @SkipMethodTrace
    protected <T> void logRuleSet(T value) {
        if (log.isTraceEnabled()) {
            log.trace("Set rule {}{} to value {}", getLogKey(), getLogLineInfo(), value);
        }
    }

    private String getLogLineInfo() {
        if (ruleContext != null && ruleContext.start != null) {
            return "({" + ruleContext.start.getLine() + "}:{" + ruleContext.start.getCharPositionInLine() + "})";
        }
        return "";
    }

    private String getLogKey() {
        String key = this.key;
        if (key.equals(UnderscoreLocation.UNDERSCORE_RULE_KEY)) {
            key = "_";
        } else if (KeyFinder.isUnnamedKey(key)) {
            key = "<no name>";
        }
        return key;
    }

    public abstract JsonValue getValue();

    public JsonValue mergeValues(Set<Rule> rules) {
        return rules.iterator().next().getValue();
    }

    public void fail() {
        requestState = RuleState.FAILED;
    }

    public boolean isRequested() {
        return requestState == RuleState.REQUESTED;
    }

    public boolean isSuccessful() {
        return requestState == RuleState.SUCCEEDED;
    }

    public boolean hasValue() {
        return isSuccessful();
    }

    public boolean isFailed() {
        return requestState == RuleState.FAILED;
    }

    public void setValue(IndexAssignment<JsonValue> ia) {
        throw new RuntimeException("IA value cannot be set on " + this);
    }
}
