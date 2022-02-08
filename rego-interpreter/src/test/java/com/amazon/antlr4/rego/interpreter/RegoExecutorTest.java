// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.junit.jupiter.api.Test;

public class RegoExecutorTest {

    private static final String TEST_PACKAGE = "MyTestPackage";
    private static final String TEST_POLICY = "package " + TEST_PACKAGE + ";";
    private static final String TEST_INPUT = "{}";

    @Test
    public void executeEmptyPolicyOnEmptyInputAndDataWithoutExceptions() throws Exception {
        new RegoExecutorBuilder(TEST_POLICY).build().executePolicy(TEST_INPUT);
    }

    @Test
    public void bailsOnPolicyError() throws Exception {
        assertThrows(
            BadPolicyException.class,
            () -> new RegoExecutorBuilder("pack abc").build().executePolicy("{}")
        );
    }

    @Test
    public void throwsRuntimeExceptionWhenGetCoverageMapIsCalledWithoutFlagsSet() {
        assertThrows(
            RuntimeException.class,
            () -> new RegoExecutorBuilder(TEST_POLICY).build().getCoverageMap()
        );
    }
}
