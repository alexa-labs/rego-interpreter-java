// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.type;

import java.util.Map;

/**
 * Interface for expressions that store data external to the current Rego execution context (e.g. file system, DB, etc.)
 * and require cleanup after the Rego evaluation is completed.
 */
public interface ContextAwarePersistentExpression extends ContextAwareExpression {
    /**
     * Remove any resources allocated by this expression. Will be run after the Rego evaluation completes.
     *
     * @param contextMap The context evaluated by the Rego expression.
     */
    void cleanup(Map<String, Object> contextMap);
}
