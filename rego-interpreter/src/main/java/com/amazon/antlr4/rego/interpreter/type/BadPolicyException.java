// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.type;

import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;

/**
 * Issue with policy syntax.
 */
public class BadPolicyException extends Exception {

    private static final long serialVersionUID = 1L;

    public BadPolicyException(Exception e) {
        super(RegoProcessorException.getContextMessage(e), e);
    }
}
