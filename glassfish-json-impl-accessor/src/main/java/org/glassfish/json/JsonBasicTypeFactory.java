package org.glassfish.json;

import java.math.BigDecimal;
import java.math.BigInteger;

import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Get access to Glassfish json implementation to rapidly create basic types.
 */
public final class JsonBasicTypeFactory {

    private JsonBasicTypeFactory() {}

    public static JsonString nativeToJson(String value) {
        return new JsonStringImpl(value);
    }

    public static JsonNumber nativeToJson(int value) {
        return JsonNumberImpl.getJsonNumber(value);
    }

    public static JsonNumber nativeToJson(long value) {
        return JsonNumberImpl.getJsonNumber(value);
    }

    public static JsonNumber nativeToJson(double value) {
        return JsonNumberImpl.getJsonNumber(value);
    }

    public static JsonNumber nativeToJson(BigInteger value) {
        return JsonNumberImpl.getJsonNumber(value);
    }

    public static JsonNumber nativeToJson(BigDecimal value) {
        return JsonNumberImpl.getJsonNumber(value);
    }

    public static JsonValue nativeToJson(boolean value) {
        return value ? JsonValue.TRUE : JsonValue.FALSE;
    }
}
