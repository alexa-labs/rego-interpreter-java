// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Rules that can have multiple values in a set.
 * See OPA documentation
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#generating-sets">here</a>.
 */
public class SetRule extends Rule {

    private Collection<JsonValue> values;

    @Override
    public Rule copy() {
        return new SetRule(key, ruleContext, values instanceof List);
    }

    public SetRule(String key, ParserRuleContext context, boolean allowDuplicates) {
        super(key, context);
        if (allowDuplicates) {
            values = new ArrayList<>();
        } else {
            values = new HashSet<>();
        }
    }

    @Override
    public void setValue(JsonValue... values) {
        this.values.addAll(Arrays.asList(values));
        super.setValue(values);
    }

    @Override
    public JsonValue mergeValues(Set<Rule> rules) {
        Set<JsonValue> values = rules.stream()
            .map(rule -> ((SetRule) rule).values)
            .flatMap(s -> s.stream())
            .collect(Collectors.toSet());
        return JsonResolver.nativeToJson(values);
    }

    @Override
    public JsonValue getValue() {
        return JsonResolver.nativeToJson(values.stream());
    }

    @Override
    public void setValue(IndexAssignment<JsonValue> ia) {
        setValue(ia.indexContext);
    }
}
