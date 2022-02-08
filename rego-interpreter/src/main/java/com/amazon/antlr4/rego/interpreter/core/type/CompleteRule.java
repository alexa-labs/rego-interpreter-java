// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Rules that can have only single value assigned to them.
 * See OPA documentation
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#complete-definitions">here</a>.
 */
public class CompleteRule extends Rule {

    private static final Predicate<Rule> HAS_DEFAULT_VALUE = rule -> rule instanceof CompleteRule
                                                            && ((CompleteRule) rule).defaultValue != null;

    private JsonValue value;
    private JsonValue defaultValue;

    public CompleteRule(String key, ParserRuleContext context) {
        super(key, context);
    }

    public CompleteRule(String key, ParserRuleContext context, JsonValue value) {
        super(key, context);
        setValue(value);
    }

    @Override
    public void setValue(JsonValue... values) {
        if (values.length > 1 || isSuccessful()) {
            throw new RegoProcessorException.UncheckedBadPolicyException("cannot set multiple values on complete rule");
        }
        value = values[0];
        super.setValue(value);
    }

    public void setDefaultValue(JsonValue value) {
        this.defaultValue = value;
    }

    public JsonValue getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public JsonValue getValue() {
        if (isSuccessful()) {
            return value;
        } else if (defaultValue != null) {
            return defaultValue;
        }
        throw new RegoProcessorException.BadReferenceException(key);
    }

    @Override
    public boolean hasValue() {
        return isSuccessful() || defaultValue != null;
    }

    @Override
    public JsonValue mergeValues(Set<Rule> rules) {
        // complete rules must not produce multiple outputs
        int countDistinctValues = ((Long) rules.stream()
                .filter(rule -> rule.hasValue() && !HAS_DEFAULT_VALUE.test(rule))
                .map(Rule::getValue)
                .distinct().count()).intValue();

        if (countDistinctValues > 1) {
            throw new RegoProcessorException.RuleEvaluationException(
                    String.format("Complete rules must not produce multiple outputs '%s'",
                                  rules.iterator().next().key));
        }

        List<JsonValue> values = rules.stream()
                .filter(Rule::hasValue)
                .map(Rule::getValue)
                .distinct()
                .collect(Collectors.toList());

        Optional<JsonValue> optionalDefaultValue = rules.stream()
                .filter(rule -> rule.hasValue() && HAS_DEFAULT_VALUE.test(rule))
                .map(rule -> ((CompleteRule) rule).defaultValue)
                .findFirst();

        // if there are some values calculated based on the conditions these are the candidates for
        // obtaining the final value, otherwise the default value will be returned.
        if (optionalDefaultValue.isPresent()) {
            JsonValue defaultValue = optionalDefaultValue.get();
            values.remove(defaultValue); // keep only the calculated values.
            if (values.size() == 0) {
                return defaultValue;
            }
        }

        return values.get(0);
    }

    @Override
    public void setValue(IndexAssignment<JsonValue> ia) {
        if (ia.assignContext != null) {
            setValue(ia.assignContext);
        } else {
            setValue(JsonValue.TRUE);
        }
    }
}
