// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.RuleManager;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipDeepMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.JsonByteArrayImpl;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException.BadReferenceException;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;

import org.glassfish.json.JsonBasicTypeFactory;

/**
 * Utility to resolve Json references to native types.
 */
public final class JsonResolver {

    private static final String INPUT = "input";
    private static final String DATA = "data";

    private JsonResolver() {}

    public static JsonValue resolveJsonRef(JsonValue state, JsonValue key) {
        if (state instanceof JsonObject) {
            return resolveJsonObjectRef((JsonObject) state, key);
        } else if (state instanceof JsonArray) {
            return resolveJsonArrayRef((JsonArray) state, key);
        }
        throw new BadReferenceException("Only array or object can be referenced");
    }

    private static JsonValue resolveJsonArrayRef(JsonArray state, JsonValue key) {
        if (key instanceof JsonNumber) {
            int index = ((JsonNumber) key).intValue();
            if (index < state.size()) {
                return state.get(index);
            }
        } else if (state.contains(key)) {
            return (JsonValue) key;
        }
        throw new BadReferenceException("JsonArray missing key: " + key);
    }

    private static JsonValue resolveJsonObjectRef(JsonObject state, JsonValue key) {
        if (!state.containsKey(jsonToString(key))) {
            throw new BadReferenceException("JsonObject missing key: " + key);
        }
        return state.get(jsonToString(key));
    }

    private static JsonBuilderFactory factory = Json.createBuilderFactory(null);

    public static String jsonToString(Object o) {
        if (o instanceof JsonString) {
            return ((JsonString) o).getString();
        } else if (o instanceof String) {
            return (String) o;
        } else if (o instanceof JsonNumber) {
            return ((JsonNumber) o).toString();
        } else if (o == JsonValue.TRUE) {
            return JsonValue.TRUE.toString();
        } else {
            return JsonValue.FALSE.toString();
        }
    }

    public static JsonValue nativeToJson(byte[] bytes) {
        return new JsonByteArrayImpl(bytes);
    }

    public static JsonValue nativeToJson(Object o) {
        if (o instanceof String) {
            return nativeToJson((String) o);
        } else if (o instanceof BigInteger) {
            return nativeToJson((BigInteger) o);
        } else if (o instanceof Long) {
            return nativeToJson((Long) o);
        } else if (o instanceof Integer) {
            return nativeToJson((Integer) o);
        } else if (o instanceof Boolean) {
            return nativeToJson(((Boolean) o).booleanValue());
        } else if (o instanceof BigDecimal) {
            return nativeToJson((BigDecimal) o);
        }
        return (JsonValue) o;
    }

    public static JsonObject nativeToJson(Map<?, ?> m) {
        JsonObjectBuilder objectBuilder = factory.createObjectBuilder();
        m.entrySet().stream().forEach(es -> {
            objectBuilder.add(jsonToString(es.getKey()), nativeToJson(es.getValue()));
        });
        return objectBuilder.build();
    }

    public static JsonValue nativeToJson(Set<?> s) {
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        s.forEach(v -> arrayBuilder.add(nativeToJson(v)));
        return arrayBuilder.build();
    }

    public static JsonValue nativeToJson(Stream<?> s) {
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        s.forEach(v -> arrayBuilder.add(nativeToJson(v)));
        return arrayBuilder.build();
    }

    public static JsonValue nativeToJson(String s) {
        return JsonBasicTypeFactory.nativeToJson(s);
    }

    public static JsonValue nativeToJson(BigDecimal b) {
        return JsonBasicTypeFactory.nativeToJson(b);
    }

    public static JsonValue nativeToJson(BigInteger d) {
        return JsonBasicTypeFactory.nativeToJson(d);
    }

    public static JsonValue nativeToJson(Long d) {
        return JsonBasicTypeFactory.nativeToJson(d);
    }

    public static JsonValue nativeToJson(Integer d) {
        return JsonBasicTypeFactory.nativeToJson(d);
    }

    public static JsonValue nativeToJson(boolean b) {
        return JsonBasicTypeFactory.nativeToJson(b);
    }

    @SkipDeepMethodTrace
    public static JsonObject extractOutput(RuleManager ruleManager, boolean withFunctions) {
        JsonObjectBuilder root = factory.createObjectBuilder();
        Map<String, Set<Rule>> rules = ruleManager.getRules(withFunctions);
        rules.entrySet().forEach(es -> {
            if (es.getKey().equals(INPUT) || es.getKey().equals(DATA)) {
                return;
            }
            addRuleTrial(root, es.getValue());
        });
        return root.build();
    }

    private static void addRuleTrial(JsonObjectBuilder root, Set<Rule> rules) {
        try {
            Rule firstRule = rules.iterator().next();
            root.add(firstRule.key, firstRule.mergeValues(rules));
        } catch (BadReferenceException e) {} // Ignore rule failures
    }

    public static JsonArray mergeArrays(JsonArray a0, JsonArray a1) {
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        a0.forEach(v -> arrayBuilder.add(v));
        a1.forEach(v -> arrayBuilder.add(v));
        return arrayBuilder.build();
    }

    public static JsonArray sliceArray(JsonArray array, int start, int end) {
        JsonArrayBuilder arrayBuilder = factory.createArrayBuilder();
        for (int i = start; i < end; i++) {
            arrayBuilder.add(array.get(i));
        }
        return arrayBuilder.build();
    }

    public static String concat(String delimiter, JsonArray values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            sb.append(stringify(values.get(i)));

            if (i != values.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String stringify(JsonValue value) {
        switch (value.getValueType()) {
            case STRING: return ((JsonString) value).getString();
            case ARRAY: return "[" + concat(", ", (JsonArray) value) + "]";
            default: return value.toString();
        }
    }
}
