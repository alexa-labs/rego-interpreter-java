// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE})
public @interface ExpressionArguments {
    ExpressionArgument[] value();
}
