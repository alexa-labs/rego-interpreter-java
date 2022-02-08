// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.type;

import java.util.List;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;

/**
 * Interface for in-built or user registered functions.
 *
 * <p>This is the Command interface in the
 * <a href="https://en.wikipedia.org/wiki/Command_pattern">Command</a> design pattern.
 * Instances of this interface are the Concrete Command types.
 */
public interface Expression extends BaseExpression {
    JsonValue run(List<JsonValue> args, JsonObject inputJson, JsonObject dataJson);
}
