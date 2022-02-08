// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import com.amazon.antlr4.rego.interpreter.core.type.SomeRule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class RuleManagerTest {

    @Test
    public void unknownRuleIsSomeRule() {
        RuleManager rr = new RuleManager();
        Assertions.assertEquals(
            SomeRule.class,
            rr.getRuleValue("unknown rule").getClass());
    }
}
