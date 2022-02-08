// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.with;

import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser.RefContext;
import com.amazon.antlr4.rego.interpreter.core.type.CompleteRule;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;

public class WithJsonResolver {

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);
    private final WithContext ctx;

    public WithJsonResolver(WithContext ctx) {
        this.ctx = ctx;
    }

    public Rule getRule(String ruleLabel) {
        JsonValue value = prepJson(ruleLabel);
        return new CompleteRule(ruleLabel, null, value);
    }

    private JsonValue prepJson(String label) {
        JsonValue refTree = ctx.ruleManager.getRuleValue(label);
        List<RefContext> rctx = ctx.lctx.with_modifier().stream()
            .filter(w -> w.term(0).ref().VAR().getText().equals(label))
            .map(w -> w.term(0).ref())
            .collect(Collectors.toList());
        return recursiveMerge(refTree, rctx, 0);
    }

    private JsonValue recursiveMerge(JsonValue refTree, List<RefContext> rctx, int rctxDepth) {
        if (rctx.size() == 0) {
            return refTree; // nothing to merge
        } else if (rctx.size() == 1 && rctx.get(0).ref_arg().size() == rctxDepth) {
            return ctx.visitor.visit(rctx.get(0).parent.parent.getChild(3)); // as term
        }
        return deepMerge(refTree, rctx, rctxDepth);
    }

    private JsonValue deepMerge(JsonValue refTree, List<RefContext> rctx, int rctxDepth) {
        JsonObject refObject = (JsonObject) refTree; // TODO: JsonArray
        JsonObjectBuilder jsonObjectBuilder = jsonBuilderFactory.createObjectBuilder();
        for (Entry<String, JsonValue> entry: refObject.entrySet()) {
            List<RefContext> matchingRctx = rctx.stream()
                .filter(r -> r.ref_arg(rctxDepth).VAR().getText().equals(entry.getKey()))
                .collect(Collectors.toList());
            JsonValue value = recursiveMerge(entry.getValue(), matchingRctx, rctxDepth + 1);
            jsonObjectBuilder.add(entry.getKey(), value);
        }
        return jsonObjectBuilder.build();
    }
}
