// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PolicyExceptionTest {

    private static final String[] BAD_RULES = {
        "badArgsIgnored := base64.decode(7)",
        "bad_type_agg2 := max(\"world\")",
        "badAll := all(\"wassup\")",
        "badAny := any(3.14)",
        "array_bad_cat_1 := array.concat([1])",
        "array_bad_cat_2 := array.concat(1, [true])",
        "array_bad_ice_2 := array.slice(1, 2, 3)",
        "boolean_and := true & true",
        "boolean_or := true | false",
        "string_and := \"a\" & \"b\"",
        "string_or := \"a\" | \"b\"",
    };

    @Test
    public void invalidRuleThrowsRuntimeException() throws Exception {
        String policyHead = "package InvalidPolicy; ";
        for (String badRule: BAD_RULES) {
            final String newLines = "\n".repeat((int) (Math.random() * 5 + 1));
            final String spaces = " ".repeat((int) (Math.random() * 5 + 1));
            final String policy = policyHead + newLines + spaces + badRule;
            BadPolicyException be = assertBadPolicy(policy);
            String msg = "@line " + (1 + newLines.length())
                + ", col " + spaces.length()
                + ", with text \""
                + badRule.split(" ")[0] + "\"";
            Assertions.assertTrue(be.getMessage().contains(msg));
        }
    }

    @Test
    public void cannotUseVariableBeforeDefinition() throws Exception {
        assertBadPolicy("package a; r { x == 1; x := 1 }");
    }

    @Test
    public void throwOnRedefinedCompleteRule() throws Exception {
        assertBadPolicy("package EmptyPolicy; a := 7; a := 8;");
    }

    @Test
    public void throwOnBadImportWithIndexedData() throws Exception {
        assertBadPolicy("package BadImport import data.array[0]");
    }

    @Test
    public void badPolicyExceptionThrownForUnknownFunctionCall() throws Exception {
        assertBadPolicy("package test; a := CallUnknownFunction(\"abc\")");
    }

    @Test
    public void testDuplicateFunctionBadPolicy() throws Exception {
        assertBadPolicy("package EmptyPolicy; f(a) = 1 { true }; f(a) = 2 { true }; r = f(0)");
    }

    @Test
    public void cannotAssignVariableTwice() throws Exception {
        assertBadPolicy("package a; q { x := 1; x := 1 }");
    }

    @Test
    public void cannotUseAssignmentWithElse() throws Exception {
        assertBadPolicy("package a; a := 7 { false } else = 6 { true }");
    }

    @Test
    public void cannotUnifyTwoUnknowns() throws Exception {
        assertBadPolicy("package a; a := x { [x, \"hello\"] = [y, \"world\"] }");
    }

    private BadPolicyException assertBadPolicy(String policy) {
        return Assertions.assertThrows(BadPolicyException.class,
            () -> new RegoExecutorBuilder(policy).build().executePolicy("{}"));
    }
}
