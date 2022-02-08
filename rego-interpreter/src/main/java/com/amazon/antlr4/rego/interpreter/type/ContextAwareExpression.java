// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.type;

import java.util.List;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;

/**
 * Interface for in-built or user registered functions that want to pass context across calls.
 *
 * <p>This is the Command interface in the
 * <a href="https://en.wikipedia.org/wiki/Command_pattern">Command</a> design pattern.
 * Instances of this interface are the Concrete Command types.
 * Context allows passing expensive (large) objects across expression calls without overhead of JSON serialization.
 *
 * @param contextMap synchronized map of objects specific to this policy execution.
 * The same map is available across all expression calls in one execution.
 * This is eligible for garbage collection at the end of policy execution.
 */
public interface ContextAwareExpression extends BaseExpression {
    JsonValue run(List<JsonValue> args, JsonObject inputJson, JsonObject dataJson, Map<String, Object> contextMap);
}
