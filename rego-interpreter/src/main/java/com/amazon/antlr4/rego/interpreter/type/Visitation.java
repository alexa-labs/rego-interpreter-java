// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.type;

public interface Visitation {
    boolean getSuccess();
    boolean getFailure();
    int getLine();
    int getColumn();
}
