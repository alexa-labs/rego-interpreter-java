// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser.RruleContext;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Rules that can have multiple key-value pairs in a map.
 * See OPA documentation
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#generating-objects">here</a>.
 */
public class ObjectRule extends Rule {

    private Map<JsonValue, JsonValue> value = new HashMap<>();

    @Override
    public Rule copy() {
        return new ObjectRule(key, ruleContext);
    }

    public ObjectRule(String key, ParserRuleContext context) {
        super(key, context);
    }

    @Override
    public JsonValue getValue() {
        return JsonResolver.nativeToJson(value);
    }

    @Override
    public void setValue(IndexAssignment<JsonValue> ia) {
        value.put(ia.indexContext, ia.assignContext);
        setValue(ia.indexContext, ia.assignContext);
    }

    @Override
    public JsonValue mergeValues(Set<Rule> rules) {
        Map<JsonValue, JsonValue> values = new HashMap<>();
        rules.forEach(r -> values.putAll(((ObjectRule) r).value));
        return JsonResolver.nativeToJson(values);
    }

    public static boolean isObjectRuleContext(RruleContext ctx) {
        return ctx.rule_definition() != null
            && ctx.rule_definition().rule_head() != null
            && ctx.rule_definition().rule_head().rule_index() != null
            && ctx.rule_definition().rule_head().rule_assignment() != null;
    }
}
