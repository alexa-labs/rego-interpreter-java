// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;

import com.amazon.antlr4.rego.interpreter.type.ContextAwarePersistentExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ExprLibraryTest {

    @Test
    public void base64DecodeTest() {
        String testData = "hello world!";
        JsonValue encodedBytes = JsonResolver.nativeToJson(Base64.getEncoder().encodeToString(testData.getBytes()));
        JsonValue decodedBytes = (JsonValue) new ExprLibrary(new HashMap<>(), null, null).executeJavaFunction("base64.decode", Arrays.asList(encodedBytes));
        Assertions.assertEquals(testData, ((JsonString) decodedBytes).getString());
    }

    @Test
    public void jsonUnmarshalTest() {
        String dateTime = new Date().toString();
        JsonValue jsonText = JsonResolver.nativeToJson("{ \"date\": { \"time\": \"" + dateTime + "\" } }");
        JsonObject jsonObject = (JsonObject) new ExprLibrary(new HashMap<>(), null, null).executeJavaFunction("json.unmarshal", Arrays.asList(jsonText));
        Assertions.assertEquals(dateTime, jsonObject.getJsonObject("date").getString("time"));
    }

    @Test
    public void arbitraryExprThrowsException() {
        Assertions.assertThrows(RegoProcessorException.UncheckedBadPolicyException.class,
            () -> new ExprLibrary(new HashMap<>(), null, null).executeJavaFunction(new Date().toString(), null));
    }

    @Test
    public void cleanupTriggers() {
        final ContextAwarePersistentExpression mockExpression = mock(ContextAwarePersistentExpression.class);
        final BaseExpression mockBaseExpression = mock(BaseExpression.class);

        final Map<String, BaseExpression> expressions = new HashMap<>();
        expressions.put("testExpression", mockExpression);
        expressions.put("testBaseExpression", mockBaseExpression);

        final ExprLibrary library = new ExprLibrary(new HashMap<>(), null, null);
        library.registerJavaFunctions(expressions);

        library.cleanupExpressions();

        verify(mockExpression).cleanup(any());
    }
}
