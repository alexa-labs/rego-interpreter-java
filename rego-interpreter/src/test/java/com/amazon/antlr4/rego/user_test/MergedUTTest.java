// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.user_test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import com.amazon.antlr4.rego.interpreter.RegoExecutor;
import com.amazon.antlr4.rego.interpreter.RegoExecutorBuilder;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MergedUTTest {

    private static final String MERGED_UT_POLICY_FILENAME = "src/test/resources/ut/MergedUTPolicy.rego";
    private static final String MERGED_UT_INPUT_FILENAME = "src/test/resources/ut/MergedUTInput.json";
    private static final String MERGED_UT_DATA_FILENAME = "src/test/resources/ut/MergedUTData.json";

    @Test
    public static void securityTests() throws Exception {
        Assertions.assertThrows(SecurityException.class, () -> new RegoExecutorBuilder(""));
        Assertions.assertThrows(SecurityException.class, () -> new RegoExecutorBuilder("").coverage(true));
    }

    @Test
    public void emptyPolicyIsGood() throws Exception {
        String policyString = "package EmptyPolicy;";
        InputStream policyStream = new ByteArrayInputStream(policyString.getBytes (StandardCharsets.UTF_8));
        InputStream inputStream = new FileInputStream(MERGED_UT_INPUT_FILENAME);
        InputStream dataStream = new FileInputStream(MERGED_UT_DATA_FILENAME);
        Assertions.assertDoesNotThrow(
            () -> new RegoExecutorBuilder(policyStream)
                .data(dataStream)
                .build()
                .executePolicy(inputStream));
    }

    private static JsonObject output = null;

    @BeforeAll
    public static void prepareMergedPolicyOutput() throws Exception {
        RegoExecutor executor = prepareExecutor(true, false);
        InputStream inputStream = prepareInputStream();
        output = executeMergedPolicy(executor, inputStream);
    }

    public static RegoExecutor prepareExecutor(boolean strictTypeCheck, boolean coverage) throws Exception {
        // Load into memory to decouple execution test SLA from disk IO.
        InputStream policyStream = new ByteArrayInputStream(Files.readAllBytes(Path.of(MERGED_UT_POLICY_FILENAME)));
        InputStream dataStream = new ByteArrayInputStream(Files.readAllBytes(Path.of(MERGED_UT_DATA_FILENAME)));
        return new RegoExecutorBuilder(policyStream)
            .data(dataStream)
            .strictTypeCheck(strictTypeCheck)
            .coverage(coverage)
            .build();
    }

    public static InputStream prepareInputStream() throws Exception {
        // Reads bytes to decouple execution time from disk IO
        return new ByteArrayInputStream(Files.readAllBytes(Path.of(MERGED_UT_INPUT_FILENAME)));
    }

    public static JsonObject executeMergedPolicy(RegoExecutor executor, InputStream inputStream) throws Exception {
        return executor.executePolicy(inputStream);
    }

    @Test
    public void testRegoOutput() {
        Assertions.assertNull(output.get("input"));
        Assertions.assertNull(output.get("data"));
    }

    @Test
    public void testImports() {
        Assertions.assertEquals("file2", output.getString("import_file2"));
        Assertions.assertEquals(98, output.getInt("import_doubleTrouble"));
        Assertions.assertEquals("f6", output.getString("import_name"));
    }

    @Test
    public void testBasicTypes() throws Exception {
        Assertions.assertEquals("hello! world", output.getString("json_string"));
        Assertions.assertFalse(output.getBoolean("json_false"));
        Assertions.assertTrue(output.getBoolean("json_true"));
        Assertions.assertEquals("raw deal", output.getString("raw_string"));
        Assertions.assertEquals(7, output.getInt("integer"));
        Assertions.assertEquals(new BigInteger("12345678901234567890"), output.getJsonNumber("large_integer").bigIntegerValue());
        Assertions.assertTrue(output.getBoolean("boolean_true"));
        Assertions.assertFalse(output.getBoolean("boolean_false"));
        Assertions.assertEquals(JsonValue.NULL, output.get("null_type"));
        Assertions.assertEquals(123, output.getInt("unification_var"));
    }

    @Test
    public void testArrayTypes() throws Exception {
        Set<Serializable> list = jsonArrayToSet(output.getJsonArray("array_type"));
        Set<Serializable> niceArr = Stream.of(true, false, null, 6, "nice").collect(Collectors.toSet());
        Set<Serializable> viceArr = Stream.of(false, null, 6, "vice", true).collect(Collectors.toSet());
        Assertions.assertEquals(niceArr, list);
        Assertions.assertNotEquals(viceArr, list);
    }

    @Test
    public void testOperatorPrecedence() throws Exception {
        Assertions.assertEquals(  1, output.getInt("op1"));
        Assertions.assertEquals(  4, output.getInt("op15"));
        Assertions.assertEquals(  2, output.getInt("op16"));
        Assertions.assertEquals(  4, output.getInt("op2"));
        Assertions.assertEquals(  4, output.getInt("op3"));
        Assertions.assertEquals(  3, output.getInt("op35"));
        Assertions.assertEquals( -6, output.getInt("op4"));
        Assertions.assertEquals( -4, output.getInt("op45"));
        Assertions.assertEquals( -3, output.getInt("op5"));
        Assertions.assertEquals(-18, output.getInt("op52"));
        Assertions.assertEquals(-22, output.getInt("op53"));
        Assertions.assertEquals(-18, output.getInt("op55"));
        Assertions.assertEquals(-10, output.getInt("op56"));
        Assertions.assertEquals(  3, output.getInt("op6"));
        Assertions.assertEquals(  3, output.getInt("op65"));
        Assertions.assertEquals( -3, output.getInt("op66"));
    }

    @Test
    public void testOperatorPrecedenceForRelations() throws Exception {
        Assertions.assertTrue(output.getBoolean("op7"));
        Assertions.assertTrue(output.getBoolean("op75"));
        Assertions.assertFalse(output.getBoolean("op76"));
        Assertions.assertFalse(output.getBoolean("op8"));
        Assertions.assertTrue(output.getBoolean("op85")); // OPA incorrectly returns false
        Assertions.assertTrue(output.getBoolean("op86"));
        Assertions.assertEquals(
            Arrays.asList(1, 2, 3),
            output.getJsonArray("op9").stream()
                .map(v -> ((JsonNumber) v).intValue())
                .sorted().collect(Collectors.toList()));
        Assertions.assertEquals(2, output.getJsonArray("op10").getInt(0));
        Assertions.assertEquals(
            Arrays.asList(1, 2),
            output.getJsonArray("op11").stream()
                .map(v -> ((JsonNumber) v).intValue())
                .sorted().collect(Collectors.toList()));
    }

    @Test
    public void testSetTypes() throws Exception {
        Set<Serializable> set = jsonArrayToSet(output.getJsonArray("set_type"));
        Set<Serializable> sixSet = Stream.of(false, null, 6, true, "nice").collect(Collectors.toSet());
        Set<Serializable> threeSet = Stream.of(true, false, null, 3, "nice").collect(Collectors.toSet());
        Assertions.assertEquals(sixSet, set);
        Assertions.assertNotEquals(threeSet, set);
        Assertions.assertEquals(0, output.getJsonArray("empty_set").size());
        Assertions.assertTrue(output.getBoolean("matching_sets"));
        Assertions.assertFalse(output.getBoolean("unmatched_sets"));
        Assertions.assertTrue(output.getBoolean("matching_sets_r"));
        Assertions.assertNull(output.get("unmatched_sets_r"));
    }

    @Test
    public void testComprehension() throws Exception {
        Assertions.assertEquals(3, output.getJsonArray("array_comprehension").size());
        Assertions.assertEquals(2, output.getJsonArray("set_comprehension").size());
        Assertions.assertEquals(2, output.getJsonObject("object_comprehension").size());
    }

    @Test
    public void testUnificationOperator() throws Exception {
        Assertions.assertEquals("hello", output.getString("unification_1"));
        Assertions.assertEquals(2, output.getInt("unification_2"));
        Assertions.assertEquals(Set.of("hello", "world"), output.getJsonArray("unification_3")
            .stream().map(v -> ((JsonString) v).getString()).collect(Collectors.toSet()));
        Assertions.assertEquals(Map.of(0, true, 1, true, 2, false, 3, false),
            output.getJsonObject("unification_4").entrySet().stream().collect(
                Collectors.toMap(
                    e -> Integer.parseInt(e.getKey()),
                    e -> e.getValue() == JsonValue.TRUE
                )
            )
        );
        Assertions.assertNull(output.get("unification_5"));
    }

    @Test
    public void testObjectTypes() throws Exception {
        JsonObject weirdType = output.getJsonObject("weird_json_object_type");
        Assertions.assertFalse(weirdType.getBoolean("true"));
        Assertions.assertTrue(weirdType.getBoolean("false"));
        Assertions.assertEquals("nice", weirdType.getString("1.3"));
        Assertions.assertNotNull(output.get("weird_json_object_type"));
        JsonObject objectType = output.getJsonObject("valid_json_object_type");
        Assertions.assertFalse(objectType.getBoolean("true"));
        Assertions.assertEquals(1.3, objectType.getJsonNumber("nice").doubleValue());
        Assertions.assertEquals(2, output.getJsonArray("port80").size());
    }

    @Test
    public void testInputDecode() throws Exception {
        Assertions.assertEquals("world", output.getString("input_data"));
        Assertions.assertEquals("hello", output.getString("decoded_str"));
        Assertions.assertNull(output.get("bad_decode"));
    }

    @Test
    public void testNegateUndefined() throws Exception {
        Assertions.assertTrue(output.getBoolean("not_undefined"));
    }

    @Test
    public void testNonExistentPath() throws Exception {
        Assertions.assertNull(output.get("non_existent_object_path_1"));
        Assertions.assertNull(output.get("non_existent_object_path_2"));
        Assertions.assertTrue(output.getBoolean("non_existent_object_path_3"));
        Assertions.assertNull(output.get("non_existent_object_path_4"));
    }

    @Test
    public void testDefaults() throws Exception {
        Assertions.assertTrue(output.getBoolean("has_actual"));
        Assertions.assertTrue(output.getBoolean("force_actual"));
        Assertions.assertTrue(output.getBoolean("has_not_actual"));
        Assertions.assertTrue(output.getBoolean("uses_default"));
    }

    @Test
    public void testIncompleteTypes() throws Exception {
        JsonArray indexedRule = output.getJsonArray("indexed_rule");
        Assertions.assertEquals(7, indexedRule.getInt(0));
        JsonArray emptyRule = output.getJsonArray("empty_partial_rule");
        Assertions.assertEquals(0, emptyRule.size());
    }

    @Test
    public void testObjectCreationExplicit() throws Exception {
        testObjectCreation("");
    }

    @Test
    public void testObjectCreationImplicit() throws Exception {
        testObjectCreation("_implicit");
    }

    private void testObjectCreation(String postfix) throws Exception {
        JsonObject objectRule = output.getJsonObject("object_rule" + postfix);
        Assertions.assertEquals(2, objectRule.getInt("file3"));
        Assertions.assertEquals("unknown index", objectRule.getString("unknown file"));
        Assertions.assertEquals("no body value", objectRule.getString("no body key"));
    }

    @Test
    public void testNegation() throws Exception {
        Assertions.assertNull(output.get("negate_truth"));
        Assertions.assertTrue(output.getBoolean("negate_false"));
        Assertions.assertNull(output.get("negate_object"));
        Assertions.assertNull(output.get("negate_array"));
        Assertions.assertNull(output.get("negate_null"));
        Assertions.assertTrue(output.getBoolean("true_null"));
    }

    @Test
    public void testBasicMath() throws Exception {
        Assertions.assertEquals(10, output.getInt("adder"));
        JsonArray subbed = output.getJsonArray("sub_partial");
        Assertions.assertEquals(4, subbed.getInt(0));
    }

    @Test
    public void testInfixOperators() throws Exception {
        Assertions.assertEquals("wassup", output.getString("infix_coverage"));
        Assertions.assertNull(output.get("infix_lies"));
        Assertions.assertEquals(6, output.getInt("modulo"));
        Assertions.assertNull(output.get("bad_modulo"));
    }

    @Test
    public void testUserFunctions() throws Exception {
        Assertions.assertEquals("userFunction1 executed", output.getString("userFunc1Result"));
        Assertions.assertEquals(98, output.getInt("userFunc2Result"));
        Assertions.assertTrue(output.getBoolean("undefinedResponse"));
    }

    @Test
    public void testDuplicateFunctions() throws Exception {
        Assertions.assertEquals(-1, output.getInt("duplicate_function_l"));
        Assertions.assertEquals(2, output.getInt("duplicate_function_t"));
    }

    @Test
    public void testFunctionWithoutAssignment() throws Exception {
        Assertions.assertNull(output.get("userFuncDefaultVal_no_match"));
        Assertions.assertTrue(output.getBoolean("userFuncDefaultVal_match"));
    }

    @Test
    public void testRuleBody() throws Exception {
        Assertions.assertTrue(output.getBoolean("rule_body_good_loops"));
        Assertions.assertNull(output.get("rule_body_bad_loops"));
        Assertions.assertNull(output.get("rule_body_bad_ref"));
        Assertions.assertNull(output.get("rule_body_unnamed_false"));
        Assertions.assertTrue(output.getBoolean("rule_body_named_false"));
    }

    @Test
    public void testArrayReferences() throws Exception {
        Assertions.assertEquals("a2a1", output.getString("array_ref"));
        Assertions.assertNull(output.get("array_ref_u"));
        Assertions.assertNotNull(output.getJsonArray("empty_array_ref"));
        Assertions.assertTrue(output.getJsonArray("empty_array_ref").isEmpty());
        Assertions.assertNotNull(output.getJsonArray("empty_array_ref_reader"));
        Assertions.assertTrue(output.getJsonArray("empty_array_ref_reader").isEmpty());
    }

    @Test
    public void testUnderscore() throws Exception {
        Set<String> fiveFiles = IntStream.rangeClosed(1, 5).mapToObj(
            c -> "file" + String.valueOf(c)).collect(Collectors.toSet());
        Set<Serializable> dataFiles = jsonArrayToSet(output.getJsonArray("file_reader"));
        Assertions.assertEquals(fiveFiles, dataFiles);

        Set<String> AToI = IntStream.rangeClosed('A', 'I').mapToObj(
            c -> Character.toString((char) c)).collect(Collectors.toSet());
        Set<Serializable> fileBytes = jsonArrayToSet(output.getJsonArray("byte_reader"));
        Assertions.assertEquals(AToI, fileBytes);

        Assertions.assertTrue(output.getBoolean("file_counts_as_expected"));
    }

    @Test
    public void testMathFunctions() throws Exception {
        Assertions.assertEquals(77, output.getInt("numop_nearest_1"));
        Assertions.assertEquals(77, output.getInt("numop_nearest_2"));
        Assertions.assertEquals(-77, output.getInt("numop_nearest_3"));
        Assertions.assertEquals(-77, output.getInt("numop_nearest_4"));
        Assertions.assertEquals(77, output.getInt("numop_ceil_1"));
        Assertions.assertEquals(77, output.getInt("numop_ceil_2"));
        Assertions.assertEquals(-76, output.getInt("numop_ceil_3"));
        Assertions.assertEquals(-76, output.getInt("numop_ceil_4"));
        Assertions.assertEquals(76, output.getInt("numop_floor_1"));
        Assertions.assertEquals(76, output.getInt("numop_floor_2"));
        Assertions.assertEquals(-77, output.getInt("numop_floor_3"));
        Assertions.assertEquals(-77, output.getInt("numop_floor_4"));
        Assertions.assertEquals(5.2, output.getJsonNumber("numop_abs_1").doubleValue());
        Assertions.assertEquals(5.2, output.getJsonNumber("numop_abs_2").doubleValue());
    }

    @Test
    public void testRangeFunctions() throws Exception {
        List<Integer> nineToTwenty = IntStream.rangeClosed(9, 20).boxed().collect(Collectors.toList());
        List<Serializable> nineToTwentyDouble = jsonArrayToList(output.getJsonArray("nine_twenty_double"));
        Assertions.assertEquals(nineToTwenty, nineToTwentyDouble);
        List<Integer> twentyToNine = IntStream.iterate(20, i -> i -1).limit(12).boxed().collect(Collectors.toList());
        List<Serializable> twentyToNineDouble = jsonArrayToList(output.getJsonArray("twenty_nine_double"));
        Assertions.assertEquals(twentyToNine, twentyToNineDouble);
        List<Integer> fifteenRange = IntStream.rangeClosed(15, 15).boxed().collect(Collectors.toList());
        List<Serializable> fifteenDouble = jsonArrayToList(output.getJsonArray("single_range"));
        Assertions.assertEquals(fifteenRange, fifteenDouble);
    }

    @Test
    public void testAggregateFunctions() throws Exception {
        Assertions.assertEquals(6, output.getInt("summer"));
        Assertions.assertEquals(6, output.getInt("winter"));
        Assertions.assertEquals(3, output.getInt("maxer"));
        Assertions.assertEquals(1, output.getInt("miner"));
        Assertions.assertEquals(10, output.getInt("mixed_agg"));
        Assertions.assertEquals(5, output.getInt("strlen"));
        Assertions.assertNull(output.get("bad_type_agg"));
        Assertions.assertEquals(3, output.getInt("good_count"));
        Assertions.assertEquals(0, output.getInt("empty_sum"));
        Assertions.assertEquals(1, output.getInt("empty_product"));
        Assertions.assertEquals(0, output.getInt("empty_count"));
        Assertions.assertNull(output.get("empty_max"));
        Assertions.assertNull(output.get("empty_min"));
    }

    @Test
    public void testAllOrAny() throws Exception {
        Assertions.assertTrue(output.getBoolean("allTrue"));
        Assertions.assertFalse(output.getBoolean("fewTrue"));
        Assertions.assertFalse(output.getBoolean("noneTrue"));
        Assertions.assertTrue(output.getBoolean("oneTrue"));
        Assertions.assertTrue(output.getBoolean("emptyAll"));
        Assertions.assertFalse(output.getBoolean("emptyAny"));
        Assertions.assertTrue(output.getBoolean("mixAllAny"));
    }

    @Test
    public void testArrayFunctions() throws Exception {
        JsonArray arrayCat = output.getJsonArray("array_cat");
        Assertions.assertEquals(4, arrayCat.size());
        Assertions.assertEquals(1, arrayCat.getInt(0));
        Assertions.assertTrue(arrayCat.getBoolean(1));
        Assertions.assertEquals("hello", arrayCat.getString(2));
        Assertions.assertFalse(arrayCat.getJsonArray(3).getBoolean(0));
        Assertions.assertEquals(0, output.getJsonArray("array_empty_cat").size());
        JsonArray arrayIce = output.getJsonArray("array_ice");
        Assertions.assertTrue(arrayIce.getBoolean(0));
        Assertions.assertEquals("hello", arrayIce.getString(1));
        Assertions.assertEquals(4, output.getJsonArray("array_bad_ice_1").size());
        Assertions.assertEquals(0, output.getJsonArray("array_bad_ice_5").size());
    }

    @Test
    public void testObjectFunctions() throws Exception {
        Assertions.assertEquals("v", output.getString("obj_key_found"));
        Assertions.assertEquals("dv", output.getString("obj_key_not_found"));
    }

    @Test
    public void testSetOperators() throws Exception {
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(1, 2, 3), output, "set1"));
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(3, 4, 5), output, "set2"));
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(3), output, "seti"));
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(3), output, "setI"));
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(1, 2, 3, 4, 5), output, "setu"));
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(1, 2, 3, 4, 5), output, "setU"));
        Assertions.assertTrue(assertIntegerSetEquals(Arrays.asList(1, 2), output, "setd"));
        Assertions.assertTrue(output.getBoolean("setC"));
    }

    @Test
    public void testElseBlocks() throws Exception {
        Assertions.assertEquals("first", output.getString("else_first"));
        Assertions.assertEquals("second", output.getString("else_second"));
        Assertions.assertEquals("third", output.getString("else_third"));
        Assertions.assertEquals("default", output.getString("else_default"));
    }

    @Test
    public void testElseForUserFunction() throws Exception {
        Assertions.assertEquals("something", output.getString("user_function_else_found"));
        Assertions.assertEquals("nothing", output.getString("user_function_else_not_found"));
        Assertions.assertEquals("e1", output.getString("user_func_multi_else_e1"));
        Assertions.assertEquals("e2", output.getString("user_func_multi_else_e2"));
        Assertions.assertEquals("whatever", output.getString("user_func_multi_else_default"));
        Assertions.assertEquals("first", output.getString("user_func_no_default_first"));
        Assertions.assertEquals("second", output.getString("user_func_no_default_second"));
        Assertions.assertNull(output.get("user_func_no_default_unknown"));
    }

    @Test
    public void testMultipleRuleDefinitions() throws Exception {
        Assertions.assertEquals(1, output.getJsonArray("conflict_rule").size());
        Assertions.assertEquals("done", output.getJsonArray("conflict_rule").getString(0));
    }

    @Test
    public void testSomeKeywordExplicit() throws Exception {
        testSomeKeyword("");
    }

    @Test
    public void testSomeKeywordImplicit() throws Exception {
        testSomeKeyword("_implicit");
    }

    private void testSomeKeyword(String postfix) throws Exception {
        Assertions.assertEquals("a1", output.getJsonArray("some_1" + postfix).getString(0));
        Assertions.assertEquals(
            Arrays.asList(1, 1),
            jsonIntegerArrayToList(output.getJsonArray("some_2" + postfix).getJsonArray(0)));
        Assertions.assertEquals(2, output.getJsonArray("tuples" + postfix).size());
        Assertions.assertEquals(
            Arrays.asList(1, 2),
            jsonIntegerArrayToList(output.getJsonArray("tuples" + postfix).getJsonArray(0)));
        Assertions.assertEquals(
            Arrays.asList(2, 1),
            jsonIntegerArrayToList(output.getJsonArray("tuples" + postfix).getJsonArray(1)));
    }

    @Test
    public void testStringFunctions() throws Exception {
        Assertions.assertEquals("a, true, false, 7, 0.8, [1, 2]", output.getString("concat_test"));
        Assertions.assertEquals("", output.getString("concat_empty_test"));
        Assertions.assertEquals("a", output.getString("concat_one_test"));
        Assertions.assertTrue(output.getBoolean("contains_true"));
        Assertions.assertFalse(output.getBoolean("contains_false"));
        Assertions.assertTrue(output.getBoolean("endswith_true"));
        Assertions.assertFalse(output.getBoolean("endswith_false"));
        Assertions.assertEquals("1000", output.getString("format_int_binary"));
        Assertions.assertEquals("10", output.getString("format_int_octal"));
        Assertions.assertEquals("8", output.getString("format_int_decimal"));
        Assertions.assertEquals("-52", output.getString("format_int_hex"));
        Assertions.assertNull(output.get("format_int_unknown"));
        Assertions.assertEquals("8de0b6b3a763ffff", output.getString("format_int_large"));
        Assertions.assertEquals(2, output.getInt("indexof_found"));
        Assertions.assertEquals(-1, output.getInt("indexof_not_found"));
        Assertions.assertEquals("ab7cd3", output.getString("lowered"));
        Assertions.assertTrue(output.getBoolean("startswith_true"));
        Assertions.assertFalse(output.getBoolean("startswith_false"));
        Assertions.assertEquals("AB7CD3", output.getString("upperred"));
        Assertions.assertEquals("", output.getString("substring_empty"));
        Assertions.assertEquals("1234", output.getString("substring_1234_1"));
        Assertions.assertEquals("1234", output.getString("substring_1234_2"));
        Assertions.assertEquals("abcd", output.getString("substring_abcd"));
        Assertions.assertNull(output.get("substring_unknown"));
    }

    @Test
    public void testOutputCanBeSerialized() throws Exception {
        Assertions.assertDoesNotThrow(() -> System.out.println(output));
    }

    private boolean assertIntegerSetEquals(List<Integer> expected, JsonObject output, String actual) {
        Set<Integer> mergedSet = new HashSet<>(expected);
        Set<Integer> actualSet = jsonIntegerArrayToSet(output.getJsonArray(actual));
        mergedSet.addAll(actualSet);
        return mergedSet.size() == expected.size();
    }

    private static List<Serializable> jsonArrayToList(JsonArray array) {
        List<Serializable> list = new ArrayList<>();
        jsonArrayToCollection(array, list);
        return list;
    }

    private static List<Integer> jsonIntegerArrayToList(JsonArray array) {
        List<Integer> list = new ArrayList<>();
        array.forEach(a -> list.add(((JsonNumber) a).intValue()));
        return list;
    }

    private static Set<Integer> jsonIntegerArrayToSet(JsonArray set) {
        Set<Integer> iSet = new HashSet<>();
        set.forEach(v -> iSet.add(((JsonNumber) v).intValue()));
        return iSet;
    }

    private static Set<Serializable> jsonArrayToSet(JsonArray array) {
        Set<Serializable> set = new HashSet<>();
        jsonArrayToCollection(array, set);
        return set;
    }

    private static void jsonArrayToCollection(JsonArray array, Collection<Serializable> c) {
        array.stream().forEach(e -> {
            if (e.getValueType() == ValueType.FALSE) {
                c.add(false);
            } else if (e.getValueType() == ValueType.TRUE) {
                c.add(true);
            } else if (e.getValueType() == ValueType.STRING) {
                c.add(((JsonString) e).getString());
            } else if (e.getValueType() == ValueType.NUMBER) {
                c.add(((JsonNumber) e).intValue());
            } else if (e.getValueType() == ValueType.NULL) {
                c.add(null);
            }
        });
    }
}
