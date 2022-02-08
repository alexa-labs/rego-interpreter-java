// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib;

import java.util.UUID;

import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;

import org.antlr.v4.runtime.ParserRuleContext;

public final class KeyFinder {

    private static final String CONTEXT_KEY_BASE = UUID.randomUUID().toString();

    private KeyFinder() {}

    public static String getKeyFromContext(ParserRuleContext ctx) {
        if (ctx instanceof RegoParser.StatContext) {
            return getStatKey((RegoParser.StatContext) ctx);
        }
        throw new RegoProcessorException.BadReferenceException("Unexpected context: " + ctx);
    }

    private static String getStatKey(RegoParser.StatContext ctx) {
        return getStatInfixKey(ctx.stat_infix());
    }

    private static String getStatInfixKey(RegoParser.Stat_infixContext ctx) {
        if (ctx.VAR() == null) {
            return KeyFinder.defaultKey(ctx.getParent());
        }
        return ctx.VAR().getText();
    }

    private static String defaultKey(ParserRuleContext ctx) {
        return CONTEXT_KEY_BASE + ctx.hashCode();
    }

    public static String getRruleKey(RegoParser.RruleContext ctx) {
        if (ctx.user_function() != null) {
            return ctx.user_function().VAR().getText();
        }
        return KeyFinder.getRuleDefinitionKey(ctx.rule_definition());
    }

    private static String getRuleDefinitionKey(RegoParser.Rule_definitionContext ctx) {
        if (ctx.DEFAULT() == null) {
            return ctx.rule_head().VAR().getText();
        }
        return defaultKey(ctx);
    }

    public static String getLiteralKey(RegoParser.LiteralContext ctx) {
        return getStatKey(ctx.stat());
    }

    public static boolean isUnnamedKey(String key) {
        return key.startsWith(CONTEXT_KEY_BASE);
    }
}
