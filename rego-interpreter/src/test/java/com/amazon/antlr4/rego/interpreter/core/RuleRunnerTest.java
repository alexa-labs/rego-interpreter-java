// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.Arrays;
import java.util.Collections;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.core.type.SetRule;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException.UncheckedBadPolicyException;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException.UndefinedRuleException;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.pattern.TokenTagToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class RuleRunnerTest {

    static class BadVisitor extends PolicyVisitor {

        private final RuntimeException e;

        BadVisitor(RuntimeException e) {
            super(null, null, JsonResolver.nativeToJson(Collections.EMPTY_MAP));
            this.e = e;
        }

        @Override
        public JsonValue visit(ParseTree tree) {
            throw e;
        }
    }

    @Test
    public void uncheckedBadPolicyExceptionsAreNotSwallowedByAllRuleRunner() {
        Assertions.assertThrows(UncheckedBadPolicyException.class,
            () -> throwFromRuleContext(new UncheckedBadPolicyException(""))
        );
    }

    @Test
    public void undefinedRuleExceptionsAreSwallowedByAllRuleRunner() {
        throwFromRuleContext(new UndefinedRuleException(""));
    }

    private void throwFromRuleContext(RuntimeException e) {
        RuleRunner rr = new RuleRunner(new BadVisitor(e), new RuleManager());
        RegoParser.RruleContext rctx = new RegoParser.RruleContext(null, 0);
        rctx.start = new TokenTagToken("tokenName", 0);
        rr.runAllRules(Arrays.asList(new SetRule("key", rctx, false)), false);
    }
}
