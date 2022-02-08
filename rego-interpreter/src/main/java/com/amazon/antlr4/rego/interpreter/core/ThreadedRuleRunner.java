// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.List;

import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;

/**
 * Executes Policy.RRule[] or Query.Literal[] definitions in sequential order.
 * Executions are scoped to a new RuleFrame but the results are collected in the current top level RuleFrame.
 * Since the runRequests need to be referenced throught this code, they are made available as a final member.
 * Which forces a new instance of this class for each list of requests.
 *
 * This class implements the <a href="https://en.wikipedia.org/wiki/Mediator_pattern">Mediator</a> design pattern.
 * A quick look at the constructor will confirm this role.
 */
class ThreadedRuleRunner {

    private final RuleManager ruleManager;
    private final PolicyVisitor visitor;
    private final List<Rule> runRequests;
    private final boolean shortCircuit;

    ThreadedRuleRunner(PolicyVisitor visitor, RuleManager ruleManager,
        List<Rule> runRequests, boolean shortCircuit
    ) {
        this.ruleManager = ruleManager;
        this.visitor = visitor;
        this.runRequests = runRequests;
        this.shortCircuit = shortCircuit;
    }

    /**
     * Each rule is registered on the top RuleFrame of the RuleStack and executed.
     * Rule execution exceptions might be cascaded or rule might be marked failed.
     */
    public void runAllRules() {
        if (runRequests.size() == 0) {
            return;
        }
        registerSuccessfulRules();
        runPendingRules();
    }

    private void runPendingRules() {
        runRequests.stream()
            .filter(Rule::isRequested)
            .forEach(rule -> handleRule(rule));
    }

    private void registerSuccessfulRules() {
        runRequests.stream()
            .filter(Rule::isSuccessful)
            .forEach(rule -> ruleManager.putRule(rule));
    }

    private void handleRule(Rule rule) {
        try {
            ruleManager.putRule(rule);
            if (!rule.isSuccessful()) {
                visitor.visit(rule.ruleContext);
            }
        } catch (RegoProcessorException.UncheckedBadPolicyException e) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                RegoProcessorException.getContextMessage(rule, e), e);
        } catch (RegoProcessorException e) {
            if (shortCircuit) {
                throw e;
            }
            rule.fail();
        } catch (RuntimeException e) {
            throw new RuntimeException(
                RegoProcessorException.getContextMessage(rule, e), e);
        }
        if (shortCircuit && !ruleManager.isScopeValid(true)) {
            throw new RegoProcessorException.RuleEvaluationException("Scope is invalid");
        }
    }
}
