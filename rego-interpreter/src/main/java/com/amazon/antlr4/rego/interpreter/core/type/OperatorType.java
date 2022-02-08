// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

/**
 * Operators selected from
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-reference/">
 * Rego reference</a>. Precedence based on
 * <a href="https://docs.oracle.com/javase/tutorial/java/nutsandbolts/operators.html">
 * Java operators</a>.
 */
public enum OperatorType {

    MULTIPLY("*", 98),
    DIVIDE("/", 98),
    MODULO("%", 98),

    ADD("+", 97),
    SUBTRACT("-", 97),

    LESS_THAN("<", 95),
    GREATER_THAN(">", 95),
    LESS_THAN_EQ("<=", 95),
    GREATER_THAN_EQ(">=", 95),

    EQUAL("==", 94),
    NOT_EQUAL("!=", 94),

    SET_AND("&", 93),

    SET_OR("|", 91),

    UNIFICATION("=", 80);

    @Getter final String code;
    @Getter final int preference;

    private static final Map<String, OperatorType> OP_MAP;

    static {
        Map<String, OperatorType> opMap = new HashMap<>();
        for (OperatorType op: OperatorType.values()) {
            opMap.put(op.code, op);
        }
        OP_MAP = Collections.unmodifiableMap(opMap);
    }

    OperatorType(String code, int preference) {
        this.code = code;
        this.preference = preference;
    }

    public static OperatorType of(String code) {
        return OP_MAP.get(code);
    }
}
