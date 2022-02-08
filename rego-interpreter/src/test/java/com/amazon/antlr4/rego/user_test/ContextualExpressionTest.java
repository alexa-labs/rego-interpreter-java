// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.RegoExecutor;
import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;
import com.amazon.antlr4.rego.interpreter.type.ContextAwareExpression;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ContextualExpressionTest {

    private static int putCount;
    private static int getCount;

    @BeforeEach
    void resetCounts() {
        getCount = putCount = 0;
    }

    private static final ContextAwareExpression CONTEXT_PUT = (args, input, data, ctx) -> {
        putCount++;
        ctx.put(ContextualExpressionTest.class.getName(), args.get(0));
        return JsonValue.TRUE;
    };

    private static final ContextAwareExpression CONTEXT_GET = (args, input, data, ctx) -> {
        getCount++;
        return (JsonValue) ctx.get(ContextualExpressionTest.class.getName());
    };

    private static final Map<String, ContextAwareExpression> RUNTIME_EXPR_MAP = Map.of(
        "test.ctx.put", CONTEXT_PUT,
        "test.ctx.get", CONTEXT_GET
    );

    private static final String POLICY1 = "package ContextExpressionTest\n"
                                        + "a {\n"
                                        + "  test.ctx.put(input.val)\n"
                                        + "  input.val == test.ctx.get()\n"
                                        + "}\n"
                                        + "b := input.val\n";

    @Test
    void badFuncThrowsExceptionToCaller() throws Exception {
        RegoExecutor executor = new RegoExecutorBuilder(POLICY1)
            .expressions(RUNTIME_EXPR_MAP)
            .build();

        for (int i=0; i<5; i++) {
            assertIntValue(executor, i);
            assertStringValue(executor, "str-" + i);
        }
    }

    // b has 4 unique values which should result in four unique put calls
    // bo1 and bo2 are same except key order
    private static final String POLICY2 = "package ContextExpressionTest\n"
                                        + "b := [0, 1, 0, 2, 0, 3]\n"
                                        + "a[ans1] {\n"
                                        + "  bi := b[_]\n"
                                        + "  bo1 := {\"k1\": bi, \"k2\": bi + 1}\n"
                                        + "  bo2 := {\"k2\": bi + 1, \"k1\": bi}\n"
                                        + "  ans1 := test.ctx.put(bo1)\n"
                                        + "  ans2 := test.ctx.put(bo2)\n"
                                        + "  test.ctx.get()\n"
                                        + "}\n"
                                        + "c := test.ctx.put({\"k1\": 0, \"k2\": 1})\n";

    @Test
    void duplicateFunctionCallsIgnored() throws Exception {
        RegoExecutor executor = new RegoExecutorBuilder(POLICY2)
            .expressions(RUNTIME_EXPR_MAP)
            .build();

        executor.executePolicy("{}");
        Assertions.assertEquals(4, putCount);
        Assertions.assertEquals(1, getCount);
    }

    private Map<String, Object> copiedContextMap = null;

    private final ContextAwareExpression CONTEXT_COPIER = (args, input, data, ctx) -> {
        copiedContextMap = ctx;
        return JsonValue.TRUE;
    };

    @Test
    void emptyContextMapCreatesNewDefaultContext() throws Exception {
        copiedContextMap = null;
        new RegoExecutorBuilder("package a; b := test.ctx.copier()")
            .expressions(Map.of("test.ctx.copier", CONTEXT_COPIER))
            .initialContextMap(null)
            .build()
            .executePolicy("{}");
        Assertions.assertNotNull(copiedContextMap);
    }

    @Test
    void resetEmptyContextMapCreatesNewDefaultContext() throws Exception {
        final Map<String, Object> testContext = new HashMap<>();
        copiedContextMap = null;
        new RegoExecutorBuilder("package a; b := test.ctx.copier()")
            .expressions(Map.of("test.ctx.copier", CONTEXT_COPIER))
            .initialContextMap(null)
            .initialContextMap(testContext)
            .initialContextMap(null)
            .build()
            .executePolicy("{}");
        Assertions.assertNotNull(copiedContextMap);
        Assertions.assertNotSame(testContext, copiedContextMap);
    }

    @Test
    void validContextMapIsCopiedToExpression() throws Exception {
        final Map<String, Object> testContext = new HashMap<>();
        copiedContextMap = null;
        new RegoExecutorBuilder("package a; b := test.ctx.copier()")
            .expressions(Map.of("test.ctx.copier", CONTEXT_COPIER))
            .initialContextMap(testContext)
            .build()
            .executePolicy("{}");
        Assertions.assertSame(testContext, copiedContextMap);
    }

    private void assertIntValue(RegoExecutor executor, int value) throws IOException, BadPolicyException {
        JsonObject output = executor.executePolicy("{\"val\": " + value + "}");
        Assertions.assertTrue(output.getBoolean("a"));
        Assertions.assertEquals(value, output.getInt("b"));
    }

    private void assertStringValue(RegoExecutor executor, String value) throws IOException, BadPolicyException {
        JsonObject output = executor.executePolicy("{\"val\": \"" + value + "\"}");
        Assertions.assertTrue(output.getBoolean("a"));
        Assertions.assertEquals(value, output.getString("b"));
    }
}
