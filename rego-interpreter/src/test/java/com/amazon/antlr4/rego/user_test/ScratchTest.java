// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.io.FileInputStream;
import java.io.InputStream;

import javax.json.JsonObject;

import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ScratchTest {

    private static final String SCRATCH_POLICY_FILENAME = "src/test/resources/scratch/ScratchPolicy.rego";
    private static final String SCRATCH_INPUT_FILENAME = "src/test/resources/scratch/ScratchInput.json";
    private static final String SCRATCH_DATA_FILENAME = "src/test/resources/scratch/ScratchData.json";

    /**
     * Move any tests to other test classes before checkin. Use this only as scratch space.
     * The associated policy/input/data/test files are ignored by git.
     */
    @Test
    void scratchTests() throws Exception {
        InputStream policyStream = new FileInputStream(SCRATCH_POLICY_FILENAME);
        InputStream inputStream = new FileInputStream(SCRATCH_INPUT_FILENAME);
        InputStream dataStream = new FileInputStream(SCRATCH_DATA_FILENAME);
        JsonObject output = new RegoExecutorBuilder(policyStream).data(dataStream).build().executePolicy(inputStream);
        Assertions.assertNotNull(output);
        // NO CODE CHANGES TO BE CHECKED INTO THIS METHOD.
        // DO NOT REMOVE THIS COMMENT.

        // TO DO: Write scratch assertions below.
    }
}
