// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import javax.json.JsonValue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UnwantedRuleOperationsTest {

    @Test
    public void ruleCannotBeCopiedByDefault() throws Exception {
        Assertions.assertThrows(RuntimeException.class, () ->
            new Rule(null, null) {
                @Override
                public JsonValue getValue() {
                    return null;
                }
            }.copy());
    }

    @Test
    public void noJsonOperationsOnUnderscoreType() throws Exception {
        assertJsonOperationsAreDisabled(new UnderscoreLocation());
        assertRuleOperationsAreDisabled(new UnderscoreLocation());
    }

    @Test
    public void noJsonOperationsOnSomeType() throws Exception {
        assertJsonOperationsAreDisabled(new SomeRule(null, null));
        assertRuleOperationsAreDisabled(new SomeRule(null, null));
    }

    private void assertRuleOperationsAreDisabled(Rule jv) {
        Assertions.assertThrows(RuntimeException.class, () -> jv.setValue(JsonValue.FALSE));
        Assertions.assertThrows(RuntimeException.class, () -> jv.copy());
    }

    private void assertJsonOperationsAreDisabled(JsonValue jv) {
        Assertions.assertThrows(RuntimeException.class, () -> jv.getValueType());
    }
}
