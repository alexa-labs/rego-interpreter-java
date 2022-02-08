// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;


public class CompleteRuleTest {

    @Test
    void completeRuleWithDefaultValueIsAccepted() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = true";

        JsonObject input = Json.createObjectBuilder().build();

        checkResult(policy, input, true);
    }

    @Test
    void whenMultipleDefaultRuleUnderTheSameKeyThenThrowsException() {
        String policy = "package test; " +
                "default allow = true " +
                "default allow = false";

        JsonObject input = Json.createObjectBuilder().build();

        checkException(policy, input, BadPolicyException.class, "rego_type_error: multiple default rules named `allow` found\n" +
                "\t@line 1, col 35, with text \"default\"");
    }

    @Test
    void completeRuleWithDefaultValueIsOverwrittenByCompleteRule() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = false " + // default value rule.
                " allow { input.v1 == true }"; //success rule

        JsonObject input = Json.createObjectBuilder()
                .add("v1", true)
                .build();

        checkResult(policy, input, true);
    }

    @Test
    void completeRuleWithDefaultValueIsReturnedWhenRuleNotMatched() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = false" + // default value rule.
                " allow { input.v1 == true }"; //failed rule

        JsonObject input = Json.createObjectBuilder()
                .add("v1", false)
                .build();

        checkResult(policy, input, false);
    }

    @Test
    void completeRuleWithDefaultValueIsOverwrittenByMatchedRule() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = 1" + // default value rule.
                " allow = 2 { input.v1 == true }"+ //failed rule
                " allow = 3 { input.v1 == false }"; //matched rule

        JsonObject input = Json.createObjectBuilder()
                .add("v1", false)
                .build();

        JsonObject result = new RegoExecutorBuilder(policy).build().executePolicy(input);
        Assertions.assertEquals(3, result.getInt("allow"));
    }


    @Test
    void completeRulesWithSameFalseValueIsAcceptable() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = true" +
                " allow { input.v1 == true }" +
                " allow { input.v2 == true }";

        JsonObject input = Json.createObjectBuilder()
                .add("v1", false)
                .add("v2", false)
                .build();

        checkResult(policy, input, true);
    }

    @Test
    void completeRulesWithSameTrueValueIsAcceptable() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = false" +
                " allow { input.v1 == true }" +
                " allow { input.v2 == true }";

        JsonObject input = Json.createObjectBuilder()
                .add("v1", true)
                .add("v2", true)
                .build();

        checkResult(policy, input, true);
    }


    @Test
    void completeRuleWithBooleanDefaultValueIsOverwrittenByMatchedRule() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = false" +
                " allow { input.v1 == true }" + // not matched
                " allow { input.v2 == true }"; // matched

        JsonObject input = Json.createObjectBuilder()
                .add("v1", false)
                .add("v2", true)
                .build();

        checkResult(policy, input, true);
    }

    @Test
    void throwsExceptionWhenConflictingOutputValues() {
        String policy = "package test; " +
                "user := \"bob\"\n" +
                "power_users := {\"alice\", \"bob\", \"fred\"}\n" +
                "restricted_users := {\"bob\", \"kim\"}\n" +

                "max_memory = 32 { power_users[user] }\n" +
                "max_memory = 4 { restricted_users[user] }";

        JsonObject input = Json.createObjectBuilder().build();
        checkException(policy, input, BadPolicyException.class, "Complete rules must not produce multiple outputs 'max_memory'");
    }

    @Test
    void throwsExceptionWhenProduceMultipleBooleanOutputValues() {
        String policy = "package test; " +
                "default allow = false" +
                " allow = true  { input.v1 == true }" + // matched
                " allow = false { input.v2 == true }"; // matched

        JsonObject input = Json.createObjectBuilder()
                .add("v1", true)
                .add("v2", true)
                .build();

        checkException(policy, input, BadPolicyException.class, "Complete rules must not produce multiple outputs 'allow'");
    }

    @Test
    void completeRuleDefaultValueIsNotConsideredConflictWithOtherValues() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = true" +
                " allow = true  { input.v1 == true }" + // not matched
                " allow = false { input.v2 == true }"; // not matched

        JsonObject input = Json.createObjectBuilder()
                .add("v1", false)
                .add("v2", false)
                .build();

        checkResult(policy, input, true);
    }

    @Test
    void completeRuleDefaultValueIsNotConsideredConflictWithOtherValues1() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = true" +
                " allow = true  { input.v1 == true }" + // matched
                " allow = false { input.v2 == true }"; // not matched

        JsonObject input = Json.createObjectBuilder()
                .add("v1", true)
                .add("v2", false)
                .build();

        checkResult(policy, input, true);
    }

    @Test
    void completeRuleDefaultValueIsNotConsideredConflictWithOtherValues2() throws BadPolicyException, IOException {
        String policy = "package test; " +
                "default allow = true" +
                " allow = true  { input.v1 == true }" + // not matched
                " allow = false { input.v2 == true }"; // matched

        JsonObject input = Json.createObjectBuilder()
                .add("v1", false)
                .add("v2", true)
                .build();

        checkResult(policy, input, false);
    }

    @Test
    void throwsExceptionWhenRedeclaration() {
        String policy = "package test; " +
                "pi := 3.14\n" +
                "pi := 3.14156";

        JsonObject input = Json.createObjectBuilder().build();
        checkException(policy, input, BadPolicyException.class, "Complete rules must not produce multiple outputs 'pi'");
    }

    private void checkResult(String policy, JsonObject input, Boolean expected) throws BadPolicyException, IOException {
        JsonObject result = new RegoExecutorBuilder(policy).build().executePolicy(input);
        Assertions.assertEquals(expected, result.getBoolean("allow"));
    }

    private <T extends Exception> void checkException(String policy, JsonObject input, Class<T> exceptionClass, String message) {
        T exception = Assertions.assertThrows(exceptionClass, () -> new RegoExecutorBuilder(policy).build().executePolicy(input));
        Assertions.assertEquals(message, exception.getMessage());
    }
}
