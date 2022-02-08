// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonArray;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser.Rule_bodyContext;
import com.amazon.antlr4.rego.RegoParser.ItermContext;
import com.amazon.antlr4.rego.RegoParser.LiteralContext;
import com.amazon.antlr4.rego.RegoParser.User_functionContext;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;
import com.amazon.antlr4.rego.interpreter.core.type.SomeRule;
import com.amazon.antlr4.rego.interpreter.core.with.WithRuleRunner;
import com.amazon.antlr4.rego.interpreter.core.type.CompleteRule;
import com.amazon.antlr4.rego.interpreter.core.type.IndexAssignment;
import com.amazon.antlr4.rego.interpreter.core.type.RegoFunction;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;

import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Provides parallel and scoped rule execution.
 *
 * <p>This class is a lightweight implementation of the
 * <a href="https://en.wikipedia.org/wiki/Facade_pattern">Facade</a> design pattern.
 */
class RuleRunner {

    private final RuleManager ruleManager;
    private final PolicyVisitor visitor;

    RuleRunner(PolicyVisitor visitor, RuleManager ruleScopeStack) {
        this.ruleManager = ruleScopeStack;
        this.visitor = visitor;
    }

    /**
     * Executes Policy.RRule[] or Query.Literal[] definitions in parallel.
     */
    public void runAllRules(List<Rule> runRequests, boolean shortCircuit) {
        new ThreadedRuleRunner(visitor, ruleManager, runRequests, shortCircuit).runAllRules();
    }

    /**
     * Run a user function with initialized arguments.
     */
    public JsonValue executeRegoFunction(String functionName, List<JsonValue> terms) {
        JsonArray functions = findFunctionsByName(functionName);
        List<JsonValue> returns = functions.stream()
            .map(f -> executeRegoFunction((RegoFunction) f, terms))
            .filter(o -> o != null)
            .collect(Collectors.toList());

        return validateFunctionReturn(functionName, returns);
    }

    private JsonValue validateFunctionReturn(String functionName, List<JsonValue> returns) {
        if (returns.size() > 1) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                "Only one value allowed from function " + functionName);
        } else if (returns.size() == 0) {
            throw new RegoProcessorException.RuleEvaluationException("Failed to evaluate " + functionName);
        }
        return returns.get(0);
    }

    private JsonArray findFunctionsByName(String functionName) {
        JsonValue rule = ruleManager.getRuleValue(functionName);
        if (rule instanceof SomeRule) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                String.format("Unknown function: %s", functionName)
            );
        }
        return (JsonArray) rule;
    }

    private JsonValue executeRegoFunction(RegoFunction userFunction, List<JsonValue> terms) {
        User_functionContext ctx = userFunction.getUserFunction();
        List<TerminalNode> argNames = ctx.dest_args().VAR();
        List<Rule> initRules = new ArrayList<>();
        for (int i = 0; i < argNames.size(); i++) {
            initRules.add(new CompleteRule(argNames.get(i).getText(), null, terms.get(i)));
        }
        return executeFunctionElse(initRules, ctx.rule_body(), ctx.iterm());
    }

    private JsonValue executeFunctionElse(List<Rule> initRules, Rule_bodyContext bctx, ItermContext ictx) {
        try {
            CompleteRule destRule = new CompleteRule(UUID.randomUUID().toString(), null);
            IndexAssignment<ParseTree> ia = new IndexAssignment<>(null, ictx);
            executeRuleContext(initRules, bctx, destRule, ia, false);
            return destRule.getValue();
        } catch (RegoProcessorException e) {
            if (bctx != null && (bctx.iterm() != null || bctx.rule_body() != null)) {
                return executeFunctionElse(initRules, bctx.rule_body(), bctx.iterm());
            }
        }
        return null;
    }

    /**
     * Executes rules in braces.
     */
    public void executeRuleContext(
        List<Rule> initRules, RuleContext ruleContext,
        Rule destRule, IndexAssignment<ParseTree> ia, boolean isPartial
    ) {
        new ScopedRuleRunner(visitor, ruleManager, initRules,
            ruleContext, destRule, ia, isPartial).executeRuleContext();
    }

    public JsonValue executeWith(LiteralContext lctx) {
        return new WithRuleRunner(visitor, ruleManager, lctx).executeWith();
    }
}
