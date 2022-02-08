// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.with;

import com.amazon.antlr4.rego.RegoParser.LiteralContext;
import com.amazon.antlr4.rego.interpreter.core.PolicyVisitor;
import com.amazon.antlr4.rego.interpreter.core.RuleManager;

public class WithContext {
    public final PolicyVisitor visitor;
    public final RuleManager ruleManager;
    public final LiteralContext lctx;

    public WithContext(PolicyVisitor visitor, RuleManager ruleManager, LiteralContext lctx) {
        this.visitor = visitor;
        this.ruleManager = ruleManager;
        this.lctx = lctx;
    }
}
