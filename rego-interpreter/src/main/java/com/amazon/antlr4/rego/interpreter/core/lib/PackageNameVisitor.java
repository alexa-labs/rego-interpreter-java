// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib;

import java.util.stream.Collectors;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoBaseVisitor;
import com.amazon.antlr4.rego.RegoParser;

public class PackageNameVisitor extends RegoBaseVisitor<JsonValue> {

    public JsonValue visitModule(RegoParser.ModuleContext ctx) {
        return JsonResolver.nativeToJson(
            ctx.rpackage().VAR().stream()
                .map(v -> v.getText())
                .collect(Collectors.joining(".")));
    }
}
