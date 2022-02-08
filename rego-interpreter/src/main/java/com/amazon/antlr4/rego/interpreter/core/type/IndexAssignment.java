// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

public class IndexAssignment<T> {

    public final T indexContext;
    public final T assignContext;

    public IndexAssignment(T ictx, T actx) {
        this.indexContext = ictx;
        this.assignContext = actx;
    }
}
