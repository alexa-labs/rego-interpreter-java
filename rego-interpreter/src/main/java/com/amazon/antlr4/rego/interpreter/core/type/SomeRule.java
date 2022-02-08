// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import javax.json.JsonValue;

import org.antlr.v4.runtime.ParserRuleContext;

/**
 * Rules that work like local variables which are automagically assigned.
 * See OPA documentation
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#some-keyword">here</a>.
 * <p>
 * <strong>Implementation Notes</strong>
 * <p>
 * Created by RuleFactory in context of a Query.
 * Ignored by RuleManager to validate scope of Query.
 * In PolicyVisitor,
 * <ul>
 * <li> When present as a Ref, the object is returned for future resolution.
 * <li> When used as a RefArg, an UnderscoreLocation is associated,
 * and last value is saved on the object for reuse. (RefArg is <code>.var</code> or <code>[var]</code>)
 * <li> Next value is generated by UnderscoreLocation semantics.
 * <li>Multi-valued resolution is <strong>NOT</strong> supported.
 * For example, <code>some i; obj[[1, i]]</code> won't work.
 * This requires walking object hierarchy and resolving all variable occurances.
 * </ul>
 */
public class SomeRule extends Rule implements JsonValue {

    private JsonValue lastValue = null;

    public SomeRule(String key, ParserRuleContext context) {
        super(key, context);
        super.setValue(this);
    }

    @Override
    public void setValue(JsonValue... values) {
        throw new RuntimeException("cannot set values on some");
    }

    @Override
    public ValueType getValueType() {
        throw new RegoProcessorException.BadReferenceException("SomeRule isn't valid JsonValue");
    }

    public void setLastValue(JsonValue value) {
        lastValue = value;
    }

    public JsonValue getLastValue() {
        if (lastValue == null) {
            return this;
        }
        return lastValue;
    }

    @Override
    public JsonValue getValue() {
        return this;
    }
}