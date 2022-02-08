// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import java.util.Objects;

import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.type.Visitation;

@SkipMethodTrace
public class VisitationImpl implements Visitation {
    private boolean success = false;
    private boolean failure = false;
    private int line, column;

    public VisitationImpl(int line, int column) {
        this.line = line;
        this.column = column;
    }

    @Override
    public boolean getSuccess() {
        return this.success;
    }

    @Override
    public boolean getFailure() {
        return this.failure;
    }

    @Override
    public int getLine() {
        return line;
    }

    @Override
    public int getColumn() {
        return column;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setFailure(boolean failure) {
        this.failure = failure;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof VisitationImpl)) {
            return false;
        }
        VisitationImpl other = (VisitationImpl) o;

        return success == other.success && failure == other.failure && line == other.line && column == other.column;
    }

    @Override
    public int hashCode() {
        return Objects.hash(success, failure, line, column);
    }
}
