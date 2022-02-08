// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class VisitationImplTest {
    private VisitationImpl v;

    @BeforeEach
    public void setUp() {
        this.v = new VisitationImpl(1, 0);
    }
    
    @Test
    public void testSameObjectEquality() {
	org.junit.jupiter.api.Assertions.assertNotNull(v);
        assertTrue(v.equals(v));
        assertTrue(v.hashCode() == v.hashCode());
    }

    @Test
    public void testDifferentTypesAreNotEqual() {
        assertFalse(v.equals(null));
        assertFalse(v.equals(new Object()));
    }

    @Test
    public void testLineColumnGetters() {
        assertEquals(1, v.getLine());
        assertEquals(0, v.getColumn());
    }

    @Test
    public void testPairsOfBooleanValues() {
        VisitationImpl other = new VisitationImpl(1, 0);
        other.setSuccess(true); // false //
        assertFalse(v.equals(other));
        other.setSuccess(false); // true true
        assertTrue(v.equals(other));
        other.setFailure(true); // true false
        assertFalse(v.equals(other));
    }

    @Test
    public void testDifferentPositionsAreNotEqual() {
        VisitationImpl differentLine = new VisitationImpl(2, 0);
        assertFalse(v.equals(differentLine));

        VisitationImpl differentCol = new VisitationImpl(1, 1);
        assertFalse(v.equals(differentCol));
    }
}
