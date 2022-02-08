// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import javax.json.JsonObject;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MultiPolicyTest {

    @Test
    public void policyOutputCarriesToNextPolicy() throws Exception {
        JsonObject output = new RegoExecutorBuilder("package a1.a2.a3 "
                                                    + "b := data.c "
                                                    + "c := data ")
            .data("{\"c\": 7, \"d\": 8}")
            .dependentBuilder(
                new RegoExecutorBuilder("package x "
                                        + "import data.a1.a2.a3.b "
                                        + "y := b "
                                        + "z := data.a1.a2.a3.c.d "
                                        + "u := data.p")
                .data("{\"p\": 9}"))
            .build()
            .executePolicy("{}");
        Assertions.assertEquals(7, output.getInt("y"));
        Assertions.assertEquals(8, output.getInt("z"));
        Assertions.assertEquals(9, output.getInt("u"));
    }

    @Test
    public void policyOutputCopiesFromFirstToSecondDependent() throws Exception {
        JsonObject output =
            new RegoExecutorBuilder("package a.b.c "
                                    + "d1 := data.d")
            .data("{\"d\": 11}")
            .dependentBuilder(
                new RegoExecutorBuilder("package a.b.c.d "
                                        + "import data.a.b.c.d1 "
                                        + "d2 := d1 "
                                        + "d3 := data.p")
                .data("{\"p\": 13}")
                .dependentBuilder(
                    new RegoExecutorBuilder("package a.b.c.e "
                                            + "import data.a.b.c.d.d1 "
                                            + "import data.a.b.c.d.d2 "
                                            + "d4 := d2 "
                                            + "d5 := data.a.b.c.d.d3 "
                                            + "d6 := data.f")
                    .data("{\"f\": 17}")))
            .build()
            .executePolicy("{}");
        Assertions.assertEquals(11, output.getInt("d1"));
        Assertions.assertEquals(11, output.getInt("d2"));
        Assertions.assertNull(output.get("d3"));
        Assertions.assertEquals(11, output.getInt("d4"));
        Assertions.assertEquals(13, output.getInt("d5"));
        Assertions.assertEquals(17, output.getInt("d6"));
    }
}
