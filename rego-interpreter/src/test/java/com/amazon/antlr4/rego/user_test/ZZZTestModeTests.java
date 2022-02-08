// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.amazon.antlr4.rego.interpreter.RegoExecutor;
import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.VisitationImpl;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;
import com.amazon.antlr4.rego.interpreter.type.Visitation;

import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Once set, test mode cannot be undone. Hence, all <i>test mode</i> tests need to be run at the end.
 * Test file execution order is set by JUnit's <code>testclass.order</code> property in build.xml.
 */
public class ZZZTestModeTests {

    @BeforeAll
    static void enableTraceAndTestMode() throws Exception {
        RegoExecutorBuilder.setTestMode(Level.TRACE);
    }

    @Test
    void runMergedUTTestsInTestMode() throws Exception {
        RegoExecutor executor = MergedUTTest.prepareExecutor(true, true);
        InputStream inputStream = MergedUTTest.prepareInputStream();
        Assertions.assertNotNull(MergedUTTest.executeMergedPolicy(executor, inputStream));
    }

    @Test
    void cannotCoverageWithoutDebugLogs() throws Exception {
        RegoExecutorBuilder.setLogLevel(Level.FATAL);
        Assertions.assertThrows(RuntimeException.class, () -> new RegoExecutorBuilder("").coverage(true));
        RegoExecutorBuilder.setLogLevel(Level.TRACE);
        Assertions.assertDoesNotThrow(() -> new RegoExecutorBuilder("").coverage(true));
    }

    @Test
    public void getCoverageMapSuccesfully() throws IOException, BadPolicyException {
        RegoExecutor regoExecutor = new RegoExecutorBuilder("package MyTestPackage;").coverage(true).build();

        assertTrue(Map.of().equals(regoExecutor.getCoverageMap()));

        regoExecutor.executePolicy("{}");
        VisitationImpl v = new VisitationImpl(1, 0);
        v.setSuccess(true);
        Map<String, Visitation> expectedMap = Map.of("line   1 col   0", v);
        Map<String, Visitation> actualMap = regoExecutor.getCoverageMap();

        assertTrue(expectedMap.equals(actualMap));
    }

    @Test
    public void cannotGetCoverageMapWithoutCoverageFlagSet() {
        RegoExecutorBuilder.setLogLevel(Level.TRACE);
        Assertions.assertThrows(
            RuntimeException.class,
            () -> new RegoExecutorBuilder("package MyTestPackage;").build().getCoverageMap()
        );
    }
}
