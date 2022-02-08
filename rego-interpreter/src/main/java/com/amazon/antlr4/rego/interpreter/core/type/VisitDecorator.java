// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;

import org.antlr.v4.runtime.tree.ParseTree;

public interface VisitDecorator {

    @SkipMethodTrace
    default void before(ParseTree tree) {}

    @SkipMethodTrace
    default void after(ParseTree tree) {}

    @SkipMethodTrace
    default void error(ParseTree tree, RuntimeException e) {}

    @SkipMethodTrace
    default void endVisit(ParseTree policyTree) {}
}
