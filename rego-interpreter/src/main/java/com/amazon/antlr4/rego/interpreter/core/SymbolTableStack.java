// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SymbolTableStack<T> {

    /**
     * This is the root scope visible to the entire policy.
     * Complete rules will be singleton sets.
     */
    private RuleScope<T> rootScope = new RuleScopeImpl<>();

    private interface RuleScope<T> extends Map<String, T> {};
    private static class RuleScopeImpl<T> extends HashMap<String, T> implements RuleScope<T> {
        private static final long serialVersionUID = 1L;
    };
    private static class RuleStack<T> extends Stack<RuleScope<T>> {
        private static final long serialVersionUID = 1L;
    };

    private RuleStack<T> ruleStack = new RuleStack<>();
    { ruleStack.add(rootScope); }

    public void pushScope() {
        ruleStack.add(new RuleScopeImpl<T>());
    }

    public void popScope() {
        ruleStack.pop();
    }

    public T get(String key) {
        RuleScope<T> ruleScope = ruleStack.peek();
        return ruleScope.get(key);
    }

    public T computeIfAbsent(String key, Function<String, T> computer) {
        RuleScope<T> frame = ruleStack.peek();
        return frame.computeIfAbsent(key, computer);
    }

    public T putIfAbsent(String key, T value) {
        RuleScope<T> frame = ruleStack.peek();
        return frame.putIfAbsent(key, value);
    }

    public T searchRuleStack(String ruleName, Supplier<T> missingSymbolProvider) {
        for (int i = ruleStack.size() - 1; i >= 0; i--) {
            RuleScope<T> frame = ruleStack.elementAt(i);
            if (frame.containsKey(ruleName)) {
                return frame.get(ruleName);
            }
        }
        return missingSymbolProvider.get();
    }

    public Map<String, T> getLocalTable() {
        return new HashMap<>(ruleStack.peek());
    }

    public Map<String, T> getGlobalTable() {
        return new HashMap<>(rootScope);
    }

    public int getStackDepth() {
        return ruleStack.size();
    }
}
