// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.amazon.antlr4.rego.interpreter.RegoExecutor;
import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PerformanceTest {

    @BeforeAll
    public static void enableLogTrace() throws Exception {
        Configurator.setRootLevel(Level.FATAL);
    }

    @Test
    public void testBasePerformance() throws Exception {
        RegoExecutor executor = new RegoExecutorBuilder("package PerfTest\na := input.a\n").build();
        final String input = "{\"a\": 7}";
        test(executor, input, 10);
        test(executor, input, 10000, Duration.ofSeconds(2));
    }

    @Test
    public void testMergedUTPerformance() throws Exception {
        RegoExecutor executor = MergedUTTest.prepareExecutor(false, false);
        InputStream inputStream = MergedUTTest.prepareInputStream();
        final String input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        test(executor, input, 10);
        test(executor, input, 1000, Duration.ofSeconds(40));
    }

    private void test(RegoExecutor executor, final String input, int count) throws IOException, BadPolicyException {
        for (int i=0; i < count; i++) {
            executor.executePolicy(input);
        }
    }

    private void test(RegoExecutor executor, final String input, int count, Duration duration) {
        Assertions.assertTimeout(duration, () -> {
            test(executor, input, count);
        });
    }
}
