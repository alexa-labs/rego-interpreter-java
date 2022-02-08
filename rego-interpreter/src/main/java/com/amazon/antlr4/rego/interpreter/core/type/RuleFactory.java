// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.ArrayList;
import java.util.List;

import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.RegoParser.RruleContext;
import com.amazon.antlr4.rego.interpreter.core.lib.KeyFinder;

import org.antlr.v4.runtime.tree.TerminalNode;

public final class RuleFactory {

    private RuleFactory() {}

    /**
     * Extract each executable Literal under Query. Unnamed literals will be
     * assigned an arbitrary name.
     */
    public static List<Rule> buildFromQuery(RegoParser.QueryContext ctx) {
        List<Rule> taskList = new ArrayList<>(ctx.literal().size());
        for (RegoParser.LiteralContext lctx : ctx.literal()) {
            if (lctx.some_decl() != null) {
                addSomeRules(taskList, lctx);
            } else {
                String key = KeyFinder.getLiteralKey(lctx);
                taskList.add(new CompleteRule(key, lctx));
            }
        }
        return taskList;
    }

    private static void addSomeRules(List<Rule> taskList, RegoParser.LiteralContext lctx) {
        for (TerminalNode t: lctx.some_decl().VAR()) {
            taskList.add(new SomeRule(t.getText(), lctx));
        }
    }

    /**
     * Extract each executable RRule under Policy. Incomplete RRule names can be
     * repeated but their definition is unique. Such duplicate RRule definitions are
     * listed as separate items.
     */
    public static List<Rule> buildFromPolicy(RegoParser.PolicyContext ctx) {
        List<Rule> taskList = new ArrayList<>(ctx.rrule().size());
        for (RegoParser.RruleContext rctx : ctx.rrule()) {
            if (rctx.user_function() != null) {
                taskList.add(new RegoFunction(KeyFinder.getRruleKey(rctx), rctx, rctx.user_function()));
            } else if (ObjectRule.isObjectRuleContext(rctx)) {
                taskList.add(new ObjectRule(KeyFinder.getRruleKey(rctx), rctx));
            } else if (isSetRuleContext(rctx)) {
                taskList.add(new SetRule(KeyFinder.getRruleKey(rctx), rctx, false));
            } else {
                taskList.add(new CompleteRule(KeyFinder.getRruleKey(rctx), rctx));
            }
        }
        return taskList;
    }

    private static boolean isSetRuleContext(RruleContext ctx) {
        return ctx.rule_definition() != null
            && ctx.rule_definition().rule_head() != null
            && ctx.rule_definition().rule_head().rule_index() != null;
    }
}
