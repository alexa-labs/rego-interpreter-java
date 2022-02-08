// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.VisitDecorator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SkipMethodTrace
public class VisitTracer implements VisitDecorator {

    @Override
    public void before(ParseTree tree) {
        if (tree instanceof ParserRuleContext) {
            ParserRuleContext ctx = (ParserRuleContext) tree;
            Token start = ctx.start;
            log.trace("Visit line {}, char {}: {}", start.getLine(), start.getCharPositionInLine(), start.getText());
        }
    }
}
