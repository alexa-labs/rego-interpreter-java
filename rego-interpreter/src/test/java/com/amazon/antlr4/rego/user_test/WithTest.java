// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import javax.json.JsonArray;
import javax.json.JsonObject;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class WithTest {

    private static final String TEST_POLICY_1 = "package WithTestPackage \n"
        + "a := [I, b, c, d, e, f, g] { \n"
        + "  I := [input.foo.bar, data.bar.foo, input.foo.car] \n"
        + "  b := input.foo.bar with data.bar.foo as 220 \n"
        + "  c := input.foo.bar with input.foo.bar as 110 with data.bar.foo as 230 \n"
        + "  d := data.bar.foo with input.foo.bar as 120 with data.bar.foo as 240 \n"
        + "  e := [input.foo.bar, data.bar.foo] with input.foo.bar as 130 with data.bar.foo as 250 \n"
        + "  f := [input.foo.bar, input.foo.car] with input.foo.bar as 140 with input.foo.car as 310 \n"
        + "  g := input.foo.bar with input.foo.car as 320 \n"
        + "} \n";
    private static final String TEST_INPUT_1 = "{"
        + "  \"foo\": {"
        + "    \"bar\": 100,"
        + "    \"car\": 300"
        + "  }"
        + "}";
    private static final String TEST_DATA_1 = "{"
    + "  \"bar\": {"
    + "    \"foo\": 200"
    + "  }"
    + "}";

    @Test
    public void directUsageOfInputAndDataOverridesAsExpected() throws Exception {
        JsonObject output = new RegoExecutorBuilder(TEST_POLICY_1)
            .data(TEST_DATA_1)
            .withKeyword(true)
            .build()
            .executePolicy(TEST_INPUT_1);
        Assertions.assertNotNull(output);
        JsonArray a = output.getJsonArray("a");
        int ai = 0;
        JsonArray I = a.getJsonArray(ai++);
        Assertions.assertEquals(100, I.getInt(0));
        Assertions.assertEquals(200, I.getInt(1));
        Assertions.assertEquals(300, I.getInt(2));
        int b = a.getInt(ai++);
        Assertions.assertEquals(100, b);
        int c = a.getInt(ai++);
        Assertions.assertEquals(110, c);
        int d = a.getInt(ai++);
        Assertions.assertEquals(240, d);
        JsonArray e = a.getJsonArray(ai++);
        Assertions.assertEquals(130, e.getInt(0));
        Assertions.assertEquals(250, e.getInt(1));
        JsonArray f = a.getJsonArray(ai++);
        Assertions.assertEquals(140, f.getInt(0));
        Assertions.assertEquals(310, f.getInt(1));
        int g = a.getInt(ai++);
        Assertions.assertEquals(100, g);
    }

    private static final String TEST_POLICY_2 = "package WithTestPackage \n"
        + "inner[true] { \n"
        + "  input.foo.bar == 110 \n"
        + "  data.bar.foo == 210 \n"
        + "} \n"
        + "middle[true] = true { \n"
        + "  count(inner) > 0 with input.foo.bar as 110 \n"
        + "} \n"
        + "outer { \n"
        + "  count(middle) > 0 with input.foo.bar as 120 with data.bar.foo as 210 \n"
        + "} \n"
        + "bad { \n"
        + "  unknown with input.foo.bar as 120 with data.bar.foo as 210 \n"
        + "} \n";
    private static final String TEST_INPUT_2 = "{"
        + "  \"foo\": {"
        + "    \"bar\": 100"
        + "  }"
        + "}";
    private static final String TEST_DATA_2 = "{"
    + "  \"bar\": {"
    + "    \"foo\": 200"
    + "  }"
    + "}";

    @Test
    public void indirectUsageOfInputAndDataOverridesAsExpected() throws Exception {
        JsonObject output = new RegoExecutorBuilder(TEST_POLICY_2)
            .data(TEST_DATA_2)
            .withKeyword(true)
            .build()
            .executePolicy(TEST_INPUT_2);
        Assertions.assertNotNull(output);
        Assertions.assertEquals(0, output.getJsonArray("inner").size());
        Assertions.assertEquals(0, output.getJsonObject("middle").size());
        Assertions.assertTrue(output.getBoolean("outer"));
        Assertions.assertNull(output.get("bad"));
    }

    @Test
    public void withKeywordRaisesException() throws Exception {
        Assertions.assertThrows(BadPolicyException.class, 
            () -> new RegoExecutorBuilder(TEST_POLICY_1)
                .data(TEST_DATA_1)
                .build()
                .executePolicy(TEST_INPUT_1));
    }
}
