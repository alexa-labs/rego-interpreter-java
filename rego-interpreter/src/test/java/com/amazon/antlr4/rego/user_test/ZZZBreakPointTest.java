// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.util.UUID;

import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.RegoBreakpointVisitor;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import lombok.Getter;

public class ZZZBreakPointTest {

    final static String TEST_POLICY = "package a; \n"
        + "b := 2 \n"
        + "c := 3 \n";

    @Test
    void breakpointIsHit() throws Exception {
        RegoExecutorBuilder.setTestMode(Level.TRACE);
        LogMatcher matcher1 = addBreakpointMatcher("Hit breakpoint on line 2, col 1, with text b:=2 of type RruleContext");
        LogMatcher matcher2 = addBreakpointMatcher("Hit breakpoint on line 2, col 6, with text 2 of type ItermContext");
        LogMatcher matcher3 = addBreakpointMatcher("Hit breakpoint on line 2, col 6, with text 2 of type TermContext");
        LogMatcher matcher4 = addBreakpointMatcher("Possible breakpoint on line 2, col 6, with text 2 of type TermContext");
        new RegoExecutorBuilder(TEST_POLICY)
            .regoBreakpoint(2, RegoParser.RruleContext.class)
            .regoBreakpoint(2, RegoParser.ItermContext.class)
            .build().executePolicy("{}");
        Assertions.assertTrue(matcher1.isMatched());
        Assertions.assertTrue(matcher2.isMatched());
        Assertions.assertFalse(matcher3.isMatched());
        Assertions.assertTrue(matcher4.isMatched());
    }

    private static LogMatcher addBreakpointMatcher(String message) {
        LogMatcher matcher = new LogMatcher(message);
        Logger logger = (Logger) LogManager.getLogger(RegoBreakpointVisitor.class);
        logger.addAppender(matcher);
        return matcher;
    }

    private static class LogMatcher extends AbstractAppender {

        final String message;
        @Getter private boolean matched = false;

        protected LogMatcher(String message) {
            super(UUID.randomUUID().toString(), null, null, true, null);
            this.message = message;
        }

        @Override
        public void append(LogEvent event) {
            if (event.getMessage().getFormattedMessage().contains(message)) {
                matched = true;
            }
        }
    }
}
