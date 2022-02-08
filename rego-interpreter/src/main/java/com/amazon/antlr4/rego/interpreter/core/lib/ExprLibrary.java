// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib;

import java.lang.reflect.AnnotatedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.RegoExecutor;
import com.amazon.antlr4.rego.interpreter.core.PolicyVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.Aggregates;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.Array;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.EncodingBase64;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.EncodingJson;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.Numbers;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.Objects;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.Set;
import com.amazon.antlr4.rego.interpreter.core.lib.expr.Strings;
import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;
import com.amazon.antlr4.rego.interpreter.core.type.ExpressionArguments;
import com.amazon.antlr4.rego.interpreter.core.type.RegoFunction;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.type.ContextAwareExpression;
import com.amazon.antlr4.rego.interpreter.type.ContextAwarePersistentExpression;
import com.amazon.antlr4.rego.interpreter.type.Expression;

/**
 * Provide execution for built-in or user registered functions.
 *
 * <p>Functions defined in Rego policy are NOT registered here. For those see {@link RegoFunction}.
 * <p>To register your functions pass them in {@link RegoExecutor}'s constructors.
 *
 * <p>This class is the Invoker in the <a href="https://en.wikipedia.org/wiki/Command_pattern">Command</a> design pattern.
 * Each Concrete Command is represented as an object of type {@link Expression}.
 *
 * <p>This class will prioritize user registered functions over built-in functions.
 * For prioritization with Rego functions see {@link PolicyVisitor#visitExpr_call()}.
 */
@SkipMethodTrace
public final class ExprLibrary {

    public final Map<String, Object> userContext;
    private final Map<String, BaseExpression> userExpressions = new HashMap<>();
    private final Map<Integer, JsonValue> executionCache = new HashMap<>();
    private final JsonObject inputJson;
    private final JsonObject dataJson;

    private static final Map<String, Expression> EXPRESSIONS = new HashMap<>();

    static {
        EXPRESSIONS.putAll(Aggregates.EXPRESSIONS);
        EXPRESSIONS.putAll(Array.EXPRESSIONS);
        EXPRESSIONS.putAll(EncodingBase64.EXPRESSIONS);
        EXPRESSIONS.putAll(EncodingJson.EXPRESSIONS);
        EXPRESSIONS.putAll(Numbers.EXPRESSIONS);
        EXPRESSIONS.putAll(Objects.EXPRESSIONS);
        EXPRESSIONS.putAll(Set.EXPRESSIONS);
        EXPRESSIONS.putAll(Strings.EXPRESSIONS);
    }

    public ExprLibrary(Map<String, Object> userContext, JsonObject inputJson, JsonObject dataJson) {
        this.userContext = userContext;
        this.inputJson = inputJson;
        this.dataJson = dataJson;
    }

    public JsonValue executeJavaFunction(String name, List<JsonValue> terms) {
        if (!isJavaFunction(name)) {
            throw new RegoProcessorException.UncheckedBadPolicyException("Unknown expression: " + name);
        }
        return cachedExecutionResults(name, terms);
    }

    private JsonValue cachedExecutionResults(String name, List<JsonValue> terms) {
        final int requestHash = hashRequest(name, terms);
        if (executionCache.containsKey(requestHash)) {
            return executionCache.get(requestHash);
        }
        JsonValue result = computeExpression(name, terms);
        executionCache.put(requestHash, result);
        return result;
    }

    private JsonValue computeExpression(String name, List<JsonValue> terms) {
        BaseExpression expr = userExpressions.get(name);
        if (expr != null) {
            return executeUserExpression(expr, terms);
        }
        return executeInBuiltExpression(name, terms);
    }

    // based on Eclipse sample code
    private int hashRequest(String name, List<JsonValue> terms) {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        result = prime * result + terms.hashCode();
        return result;
    }

    private JsonValue executeInBuiltExpression(String name, List<JsonValue> terms) {
        BaseExpression expr = EXPRESSIONS.get(name);
        validateExpressionArguments(expr, terms);
        try {
            return ((Expression) expr).run(terms, inputJson, dataJson);
        } catch (ClassCastException | IndexOutOfBoundsException e) {
            throw new RegoProcessorException.UncheckedBadPolicyException(
                name + ", " + e.getMessage(), e);
        }
    }

    private JsonValue executeUserExpression(BaseExpression expr, List<JsonValue> terms) {
        validateExpressionArguments(expr, terms);
        if (expr instanceof Expression) {
            return ((Expression) expr).run(terms, inputJson, dataJson);
        }
        return ((ContextAwareExpression) expr).run(terms, inputJson, dataJson, userContext);
    }

    /**
     * Triggers cleanup for all expressions. Should be run after the Rego evaluation has completed.
     */
    public void cleanupExpressions() {
        for (final BaseExpression expression : userExpressions.values()) {
            if (expression instanceof ContextAwarePersistentExpression) {
                ((ContextAwarePersistentExpression) expression).cleanup(userContext);
            }
        }
    }

    private boolean strictTypeCheckEnabled = false;

    public void strictTypeCheckEnabled(boolean strictTypeCheckEnabled) {
        this.strictTypeCheckEnabled = strictTypeCheckEnabled;
    }

    public boolean strictTypeCheckEnabled() {
        return strictTypeCheckEnabled;
    }

    private void validateExpressionArguments(BaseExpression expr, List<JsonValue> actual) {
        if (!strictTypeCheckEnabled()) {
            return;
        }
        AnnotatedType[] ai = expr.getClass().getAnnotatedInterfaces();
        for (AnnotatedType at: ai) {
            ExpressionArguments expected = at.getAnnotation(ExpressionArguments.class);
            if (expected != null) {
                matchArgumentTypes(expected, actual);
            }
        }
    }

    private void matchArgumentTypes(ExpressionArguments expected, List<JsonValue> actual) {
        List<Class<? extends JsonValue>> expectedTypes = Stream.of(expected.value())
            .map(ea -> ea.value())
            .collect(Collectors.toList());
        matchArgumentTypes(expectedTypes, actual);
    }

    public static void matchArgumentTypes(List<Class<? extends JsonValue>> expectedTypes, List<JsonValue> actual) {
        if (!classTypesMatch(expectedTypes, actual)) {
            List<Class<? extends JsonValue>> actualTypes = actual.stream()
                .map(a -> a.getClass())
                .collect(Collectors.toList());
            throw new RegoProcessorException.UncheckedBadPolicyException(
                "Argument type mismatch. Expected: " + expectedTypes + ", Actual: " + actualTypes);
        }
    }

    private static boolean classTypesMatch(List<Class<? extends JsonValue>> base, List<JsonValue> extn) {
        if (base.size() != extn.size()) {
            return false;
        }
        for (int i = 0; i < base.size(); i++) {
            if (!base.get(i).isInstance(extn.get(i))) {
                return false;
            }
        }
        return true;
    }

    public boolean isJavaFunction(String name) {
        return userExpressions.get(name) != null || EXPRESSIONS.get(name) != null;
    }

    public void registerJavaFunctions(Map<String, BaseExpression> exprMap) {
        userExpressions.putAll(exprMap);
    }
}
