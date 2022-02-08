// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.PolicyVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.VisitDecorator;

import org.antlr.v4.runtime.tree.ParseTree;

public class DecoratedVisitor extends PolicyVisitor {

    private List<VisitDecorator> decorators = Collections.emptyList();

    public DecoratedVisitor(Map<String, Object> initialContextMap, JsonObject input, JsonObject data) {
        super(initialContextMap, input, data);
    }

    @Override
    @SkipMethodTrace
    public JsonValue visit(ParseTree tree) {
        if (tree == null) {
            return null;
        }
        decorators.forEach(d -> d.before(tree));
        try {
            JsonValue res = super.visit(tree);
            decorators.forEach(d -> d.after(tree));
            return res;
        } catch (RuntimeException e) {
            decorators.forEach(d -> d.error(tree, e));
            throw e;
        }
    }

    @SkipMethodTrace
    public DecoratedVisitor withDecorators(List<VisitDecorator> decorators) {
        this.decorators = decorators;
        return this;
    }
}
