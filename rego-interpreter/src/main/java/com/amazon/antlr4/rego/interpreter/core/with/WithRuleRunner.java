// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.with;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser.LiteralContext;
import com.amazon.antlr4.rego.interpreter.core.PolicyVisitor;
import com.amazon.antlr4.rego.interpreter.core.RuleManager;

/**
 * With keyword requires all dependencies to be re-evaluated.
 * This can be optimized to skip reevaluation of rules that are not impacted by input/data change.
 */
public class WithRuleRunner {

    private final WithContext externalContext;
    private final WithContext internalContext;

    public WithRuleRunner(PolicyVisitor visitor, RuleManager ruleManager, LiteralContext lctx) {
        externalContext = new WithContext(visitor, ruleManager, lctx);
        internalContext = createWithContext(externalContext);
    }

    private WithContext createWithContext(WithContext externalContext) {
        WithRuleManager ruleManager = new WithRuleManager(externalContext);
        PolicyVisitor visitor = new PolicyVisitor(externalContext.visitor, ruleManager);
        WithContext internalContext = new WithContext(visitor, ruleManager, externalContext.lctx);
        ruleManager.withInternalContext(internalContext);
        return internalContext;
    }

    /**
     * Reset the stack wth new copies of input and data.
     * Any dependencies are recomputed
     */
    public JsonValue executeWith() {
        if (internalContext.lctx.stat().stat_infix() != null) {
            return internalContext.visitor.visit(internalContext.lctx.stat().stat_infix());
        }
        return internalContext.visitor.visit(internalContext.lctx.stat().term());
    }
}
