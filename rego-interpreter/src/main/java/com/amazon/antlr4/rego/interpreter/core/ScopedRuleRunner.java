// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.List;

import com.amazon.antlr4.rego.interpreter.core.type.IndexAssignment;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;
import com.amazon.antlr4.rego.interpreter.core.type.UnderscoreLocation;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Executes Rule_body by setting up local RuleFrames, managing underscores, and extracting results.
 * Note that a Rule_body references Query.Literal[] which will lead to further scopes.
 * Since several variables need to be referenced throught this code, they are made available as final members.
 * Which forces a new instance of this class for each execution of a Rule_body.
 *
 * This class implements the <a href="https://en.wikipedia.org/wiki/Mediator_pattern">Mediator</a> design pattern.
 * A quick look at the constructor will confirm this role.
 */
class ScopedRuleRunner {

    private final RuleManager ruleManager;
    private final PolicyVisitor visitor;
    private final List<Rule> initRules;
    private final RuleContext ruleContext;
    private final Rule destRule;
    private final IndexAssignment<ParseTree> ia;
    private final boolean isPartial;
    private final UnderscoreLocation underscore = new UnderscoreLocation();

    ScopedRuleRunner(PolicyVisitor visitor, RuleManager ruleScopeStack, List<Rule> initRules,
        RuleContext ruleContext, Rule destRule, IndexAssignment<ParseTree> ia, boolean isPartial
    ) {
        this.ruleManager = ruleScopeStack;
        this.visitor = visitor;
        this.initRules = initRules;
        this.ruleContext = ruleContext;
        this.destRule = destRule;
        this.ia = ia;
        this.isPartial = isPartial;
    }

    /**
     * Run rules in a bubble:
     * In loop for _ variables:
     *   Initialize stack with initial rules,
     *   Process body + reference,
     *   Pop stack and save results.
     */
    public void executeRuleContext() {
        runInLocalScope(underscore);
        executeUnderscoreLoops();
        if (!destRule.hasValue()) {
            if (!isPartial) {
                throw new RegoProcessorException.RuleEvaluationException("No results for rule: " + destRule.key);
            }
            destRule.setValue();
        }
    }

    private void executeUnderscoreLoops() {
        while (underscore.isReadyForNext()) {
            runInLocalScope(underscore);
        }
    }

    private void runInLocalScope(UnderscoreLocation job) {
        ruleManager.pushScope(initRules);
        try {
            ruleManager.putRule(job);
            if (ruleContext != null) {
                visitor.visit(ruleContext);
            }
            if (ruleManager.isScopeValid()) {
                destRule.setValue(visitor.resolveIndexAssignment(ia));
            }
        } catch (RegoProcessorException.UncheckedBadPolicyException e) {
            throw e;
        } catch (RegoProcessorException e) {
        } finally {
            ruleManager.popScope();
        }
    }
}
