// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.Set;
import java.util.stream.Collectors;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser.RruleContext;
import com.amazon.antlr4.rego.RegoParser.User_functionContext;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.type.Expression;

/**
 * This class provides an executable reference to a function implemented in Rego policy.
 * See OPA documentation
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#functions">here</a>.
 * <p>
 * <strong>Implementation Notes</strong>
 * <p>
 * <p>For the same reasons as {@link UnderscoreLocation}, this class implements the
 * <a href="https://en.wikipedia.org/wiki/Adapter_pattern">Adapter</a> design pattern.
 *
 * <p>The adaptee is a reference to a function defined in Rego policy. This Rego function follows the
 * <a href="https://en.wikipedia.org/wiki/Command_pattern">Command</a> design pattern,
 * similar to an {@link Expression}.
 */
public class RegoFunction extends Rule implements JsonValue {

    private final RruleContext rctx;

    public RegoFunction(String key, RruleContext rctx, User_functionContext ufc) {
        super(key, rctx);
        this.rctx = rctx;
        super.setValue(this);
    }

    public User_functionContext getUserFunction() {
        return rctx.user_function();
    }

    public JsonValue mergeValues(Set<Rule> rules) {
        return JsonResolver.nativeToJson(
            rules.stream().map(r -> (RegoFunction) r).collect(Collectors.toSet()));
    }

    @Override
    public ValueType getValueType() {
        throw new RegoProcessorException.BadReferenceException("UserFunction isn't valid JsonValue");
    }

    @Override
    public void setValue(JsonValue... values) {
        throw new RuntimeException("cannot set values on function");
    }

    @Override
    public JsonValue getValue() {
        return this;
    }
}
