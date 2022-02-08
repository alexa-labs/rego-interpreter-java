// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import java.util.Set;

import com.amazon.antlr4.rego.interpreter.core.type.VisitDecorator;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class RegoBreakpointVisitor implements VisitDecorator {

    private final Set<String> breakpoints;

    public RegoBreakpointVisitor(Set<String> regoBreakpoints) {
        this.breakpoints = regoBreakpoints;
    }

    @Override
    public void before(ParseTree tree) {
        if (tree instanceof ParserRuleContext) {
            ParserRuleContext context = (ParserRuleContext) tree;
            final String contextKey = context.start.getLine() + context.getClass().getName();
            final String breakpointDesc = String.format(
                "breakpoint on line %d, col %d, with text %s of type %s",
                context.start.getLine(), context.start.getCharPositionInLine() + 1,
                context.getText(), context.getClass().getSimpleName());
            if (breakpoints.contains(contextKey)) {
                // SETUP BREAKPOINT HERE IN YOUR PREFERRED DEBUGGER
                log.debug("Hit {}", breakpointDesc);
            } else {
                log.trace("Possible {}", breakpointDesc);
            }
        }
    }
}
