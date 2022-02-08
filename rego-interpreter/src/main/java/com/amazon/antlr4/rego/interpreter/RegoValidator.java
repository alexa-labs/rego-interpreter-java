// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter;

import java.io.IOException;
import java.util.EnumSet;

import com.amazon.antlr4.rego.interpreter.core.PolicyValidationVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.misc.ParseCancellationException;

/**
 * Provide basic policy validation which can be performed without looking at the input.
 */
@SkipMethodTrace
public final class RegoValidator {

    public enum ValidationOptions {
        ALLOW_WITH_KEYWORD
    }

    private RegoValidator() {}

    public static void validatePolicy(String policy) throws BadPolicyException {
        validatePolicy(policy, EnumSet.noneOf(ValidationOptions.class));
    }

    public static void validatePolicy(String policy, EnumSet<ValidationOptions> options) throws BadPolicyException {
        try {
            new PolicyValidationVisitor(options).visit(
                RegoExecutor.prepPolicyTree(
                    CharStreams.fromStream(
                        RegoExecutorBuilder.stringToInputStream(policy))));
        } catch (IOException | ParseCancellationException | RegoProcessorException e) {
            throw new BadPolicyException(e);
        }
    }
}
