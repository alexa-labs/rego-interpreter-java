// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Token;

/**
 * Base class for all RegoProcessor exceptions.
 */
public abstract class RegoProcessorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private RegoProcessorException(String s) {
        super(s);
    }

    private RegoProcessorException(String s, Throwable e) {
        super(s, e);
    }

    /**
     * Bad policy
     */
    public static class UncheckedBadPolicyException extends RegoProcessorException {

        private static final long serialVersionUID = 1L;

        public UncheckedBadPolicyException(String s) {
            super(s);
        }

        public UncheckedBadPolicyException(String s, Throwable e) {
            super(s, e);
        }
    }

    /**
     * Reference points to non-existent member.
     */
    public static class BadReferenceException extends RegoProcessorException {

        private static final long serialVersionUID = 1L;

        public BadReferenceException(String s) {
            super(s);
        }

        public BadReferenceException(String s, Throwable e) {
            super(s, e);
        }
    }

    /**
     * In Rego, null and undefined are not the same thing.
     * We indicate undefined with this exception.
     */
    public static class UndefinedRuleException extends RegoProcessorException {

        private static final long serialVersionUID = 1L;

        public UndefinedRuleException(String ruleName) {
            super(ruleName);
        }
    }

    /**
     * Within a local scope, all evaluations must be successful.
     * This exception indicates that at least one failed.
     */
    public static class RuleEvaluationException extends RegoProcessorException {

        private static final long serialVersionUID = 1L;

        public RuleEvaluationException(String s) {
            super(s);
        }
    }

    public static String getContextMessage(Throwable t) {
        if (t instanceof RecognitionException) {
            RecognitionException re = (RecognitionException) t;
            return getContextMessage(re.getOffendingToken());
        } else if (t.getMessage() == null && t.getCause() != null) {
            return getContextMessage(t.getCause());
        }
        return t.getMessage();
    }

    public static String getContextMessage(Rule runRequest, Throwable e) {
        return e.getMessage() + "\n\t@" + getContextMessage(runRequest.ruleContext.start);
    }

    private static String getContextMessage(Token token) {
        return String.format("line %d, col %d, with text \"%s\"",
            token.getLine(),
            token.getCharPositionInLine(),
            token.getText());
    }
}
