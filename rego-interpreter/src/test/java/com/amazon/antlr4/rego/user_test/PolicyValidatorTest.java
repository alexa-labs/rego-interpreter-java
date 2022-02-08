// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.util.EnumSet;

import com.amazon.antlr4.rego.interpreter.RegoValidator;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PolicyValidatorTest {

    @Test
    public void missingPackageIsValidationError() throws Exception {
        Assertions.assertThrows(BadPolicyException.class, () -> RegoValidator.validatePolicy("a := 1\n"));
    }

    @Test
    public void packageOnlyPolicyIsOk() throws Exception {
        Assertions.assertDoesNotThrow(() -> RegoValidator.validatePolicy("package test\n"));
    }

    private static final String WITH_POLICY = "package WithTest\n"
        + "a {\n"
        + "  true\n"
        + "}\n"
        + "b {\n"
        + "  a with input.foo as 7\n"
        + "}\n";

    @Test
    public void notExplicitlyRequestingWithKeywordThrowsException() throws Exception {
        Assertions.assertThrows(BadPolicyException.class, () -> RegoValidator.validatePolicy(WITH_POLICY));
    }

    @Test
    public void explicitRequestForWithKeywordPassesValidation() throws Exception {
        Assertions.assertDoesNotThrow(() -> RegoValidator.validatePolicy(
            WITH_POLICY, EnumSet.of(RegoValidator.ValidationOptions.ALLOW_WITH_KEYWORD)));
    }
}
