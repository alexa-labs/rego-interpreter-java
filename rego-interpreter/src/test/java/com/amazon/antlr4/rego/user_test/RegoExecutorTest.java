// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.json.JsonObject;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RegoExecutorTest {

    private static final String TEST_PACKAGE = "MyTestPackage";
    private static final String TEST_POLICY = "package " + TEST_PACKAGE + ";";
    private static final String TEST_INPUT = "{}";
    private static final String TEST_DATA = "{}";

    @Test
    public void canGetPolicyOutput() throws Exception {
        String assignmentPolicy = TEST_POLICY + "a := 7;";
        JsonObject output = new RegoExecutorBuilder(assignmentPolicy)
            .data(TEST_DATA)
            .build()
            .executePolicy(TEST_INPUT);
        Assertions.assertNotNull(output);
        Assertions.assertEquals(7, output.getInt("a"));
    }

    @Test
    public void stringPolicyWithoutTerminalCharacterWorks() throws Exception {
        String policy = "package Test; a := 7";
        Assertions.assertDoesNotThrow(() -> new RegoExecutorBuilder(policy).build());
    }

    @Test
    public void streamPolicyWithoutTerminalCharacterWorks() throws Exception {
        InputStream policy = new ByteArrayInputStream(("package Test; a := 7".getBytes(StandardCharsets.UTF_8)));
        Assertions.assertDoesNotThrow(() -> new RegoExecutorBuilder(policy).build());
    }

    @Test
    public void cannotCoverageWithoutTestMode() throws Exception {
        Assertions.assertThrows(SecurityException.class, 
            () -> new RegoExecutorBuilder("package a").coverage(true));
    }

    @Test
    public void canDisbaleCoverageWithoutTestMode() throws Exception {
        Assertions.assertDoesNotThrow(() -> new RegoExecutorBuilder("package a").coverage(false));
    }

    @Test
    public void cannotEnableDebugOrTraceLogsInProdMode() throws Exception {
        RegoExecutorBuilder.setLogLevel(Level.TRACE);
        Assertions.assertThrows(SecurityException.class, () -> new RegoExecutorBuilder("package a"));
        RegoExecutorBuilder.setLogLevel(Level.DEBUG);
        Assertions.assertThrows(SecurityException.class, () -> new RegoExecutorBuilder("package a"));
        RegoExecutorBuilder.setLogLevel(Level.WARN);
        Assertions.assertDoesNotThrow(() -> new RegoExecutorBuilder("package a"));
    }
}
