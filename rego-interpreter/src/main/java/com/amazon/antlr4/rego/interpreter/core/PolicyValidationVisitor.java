// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.EnumSet;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoBaseVisitor;
import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.interpreter.RegoValidator.ValidationOptions;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;

public class PolicyValidationVisitor extends RegoBaseVisitor<JsonValue> {

    private final EnumSet<ValidationOptions> options;

    public PolicyValidationVisitor(EnumSet<ValidationOptions> options) {
        this.options = options;
    }

    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitWith_modifier(RegoParser.With_modifierContext ctx) {
        if (!options.contains(ValidationOptions.ALLOW_WITH_KEYWORD)) {
            throw new RegoProcessorException.UncheckedBadPolicyException("With keyword not supported");
        }
        return super.visitWith_modifier(ctx);
    }
}
