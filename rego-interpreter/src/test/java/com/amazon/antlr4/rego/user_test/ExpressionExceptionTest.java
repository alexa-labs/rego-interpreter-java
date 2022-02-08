// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.stream.IntStream;

import com.amazon.antlr4.rego.interpreter.RegoExecutor;
import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.Expression;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpressionExceptionTest {

    private static int exceptionIndex = 0;

    private static final Expression RUNTIME_ERR = (args, input, data) -> {
        switch(exceptionIndex++ % 3) {
            case 0: throw new RuntimeException("BadFunc throws RuntimeException");
            case 1: throw new ClassCastException("BadFunc throws ClassCastException");
            default: throw new IndexOutOfBoundsException("BadFunc throws IndexOutOfBoundsException");
        }
    };

    private static final Map<String, Expression> RUNTIME_EXPR_MAP = Map.of(
        "BadFunc", RUNTIME_ERR
    );

    private static final String POLICY1 = "package BadFunctionPolicy\n"
                                        + "a := BadFunc()\n";

    private static final String POLICY2 = "package BadFunctionPolicy\n"
                                        + "a {\n"
                                        + "  b := BadFunc()\n"
                                        + "}\n";

    private static final String POLICY3 = "package BadFunctionPolicy\n"
                                        + "a {\n"
                                        + "  b := [1, 2, 3, 4, 5][_]"
                                        + "  c := BadFunc(b)\n"
                                        + "}\n";

    private static final String POLICY4 = "package BadFunctionPolicy\n"
                                        + "RegoFunc() = ret {\n"
                                        + "  ret := BadFunc()\n"
                                        + "}\n"
                                        + "a := RegoFunc()\n";

    private static final String POLICY5 = "package BadFunctionPolicy\n"
                                        + "a {\n"
                                        + "  true == false\n"
                                        + "  ret := BadFunc()\n"
                                        + "}\n";

    private static final String POLICY6 = "package BadArgType "
                                        + "a := contains(\"a\", 7)";

    private static final String POLICY7 = "package BadArgCount "
                                        + "a := contains(\"a\", \"b\", \"c\")";

    private static final String POLICY8 = "package BadCount "
                                        + "a := count(5)";

    @Test
    void badFuncThrowsExceptionToCaller() throws Exception {
        runtimeTestWithPolicy(POLICY1, "\t@line 2, col 0, with text \"a\"");
    }

    @Test
    void badFuncInsideRuleBodyThrowsExceptionToCaller() throws Exception {
        runtimeTestWithPolicy(POLICY2,
            "\t@line 2, col 0, with text \"a\"",
            "\t@line 3, col 2, with text \"b\"");
    }

    @Test
    void badFuncInsideUnderscoreThrowsExceptionToCaller() throws Exception {
        runtimeTestWithPolicy(POLICY3,
            "\t@line 2, col 0, with text \"a\"",
            "\t@line 3, col 27, with text \"c\"");
    }

    @Test
    void badFuncInsideRegoFunctionThrowsExceptionToCaller() throws Exception {
        runtimeTestWithPolicy(POLICY4,
            "\t@line 3, col 2, with text \"ret\"",
            "\t@line 5, col 0, with text \"a\"");
    }

    private void runtimeTestWithPolicy(String policy, String... errorMessages) {
        IntStream.range(0, 3).forEach(i -> {
            RuntimeException re = Assertions.assertThrows(RuntimeException.class,
                () -> buildWithPolicyAndExpression(policy, RUNTIME_EXPR_MAP).executePolicy("{}"));
            for (String msg: errorMessages)  {
                Assertions.assertTrue(re.getMessage().contains(msg));
            }
        });
    }

    @Test
    void falseRuleWillShortCircuitBadRule() throws Exception {
        Assertions.assertNull(buildWithPolicyAndExpression(POLICY5, RUNTIME_EXPR_MAP).executePolicy("{}").get("a"));
    }

    @Test
    void incorrectArgumentTypeThrowsBadPolicyException() throws Exception {
        Assertions.assertThrows(BadPolicyException.class,
            () -> buildWithPolicyAndExpression(POLICY6, Collections.emptyMap(), true).executePolicy("{}"));
    }

    @Test
    void incorrectArgumentTypeThrowsBadPolicyExceptionWithoutStrictTypeCheck() throws Exception {
        Assertions.assertThrows(BadPolicyException.class,
            () -> buildWithPolicyAndExpression(POLICY6, Collections.emptyMap(), false).executePolicy("{}"));
    }

    @Test
    void incorrectArgumentCountThrowsBadPolicyException() throws Exception {
        Assertions.assertThrows(BadPolicyException.class,
            () -> buildWithPolicyAndExpression(POLICY7, Collections.emptyMap(), true).executePolicy("{}"));
    }

    @Test
    void incorrectArgumentCountIgnoredWithoutStrictTypeCheck() throws Exception {
        Assertions.assertNotNull(buildWithPolicyAndExpression(POLICY7, Collections.emptyMap(), false).executePolicy("{}"));
    }

    @Test
    void countFunctionThrowsIfCountingNonListOrString() throws Exception {
        Assertions.assertThrows(BadPolicyException.class,
            () -> buildWithPolicyAndExpression(POLICY8, Collections.emptyMap(), true).executePolicy("{}"));
    }

    private RegoExecutor buildWithPolicyAndExpression(String policy, Map<String, Expression> expr) throws IOException, BadPolicyException {
        return buildWithPolicyAndExpression(policy, expr, false);
    }

    private RegoExecutor buildWithPolicyAndExpression(String policy, Map<String, Expression> expr, boolean strictTypeCheck) throws IOException, BadPolicyException {
        return new RegoExecutorBuilder(policy).expressions(expr).strictTypeCheck(strictTypeCheck).build();
    }
}
