# Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
# SPDX-License-Identifier: Apache-2.0

package MergedUTPolicy

import data.files[0] as file2
import data.doubleTrouble
import data.file_sizes[5].name

import_file2 := file2
import_doubleTrouble := doubleTrouble
import_name := name

json_string := "hello! world"
json_false := data["false"]
json_true := data["true"]
raw_string := `raw deal`
integer := 7
large_integer := 12345678901234567890
boolean_true := true
boolean_false := false
null_type := null
unification_var = 123
array_type := [true, false, null, 6, `nice`]
set_type := {true, false, null, 6, `nice`}
empty_set := set()
weird_json_object_type := {true: false, 1.3: `nice`, false: true}
valid_json_object_type := {"true": false, `nice`: 1.3}

ips_by_port := {
    80: ["1.1.1.1", "1.1.1.2"],
    443: ["2.2.2.1"],
}
port80 := ips_by_port[80]

input_data := input.data.hello
decoded_str := base64.decode(input.event.metadata)
bad_decode := base64.decode("abc,")
not_undefined {
    not bad_decode
}

non_existent_object_path_1 {
    input.notexist
}
non_existent_object_path_2 {
    input.data.notexist
}
non_existent_object_path_3 {
    not input.data.notexist
}
non_existent_object_path_4 := input.data.notexist

default has_actual = false
has_actual {
	1 == 1
}

force_actual = true {
    true
}

default has_not_actual = true
uses_default {
    has_not_actual
}

indexed_rule[index] {
    index := 7
}

empty_partial_rule[p] {
    p := 1
    false
}

object_rule[k] := v {
    some v
    k := data.files[v]
}

object_rule[k] := v {
    k := "unknown file"
    v := "unknown index"
}

object_rule["no body key"] := "no body value";

object_rule_implicit[k] := v {
    k := data.files[v]
}

object_rule_implicit[k] := v {
    k := "unknown file"
    v := "unknown index"
}

object_rule_implicit["no body key"] := "no body value";

negate_truth {
    not 1 < 2
}

negate_false {
    not 1 > 2
}

negate_object {
    not {}
}

negate_array {
    m := []
    not m
}

negate_null {
    not null
}

true_null {
    null
}

adder := added {
    added := 7 + 3
}

sub_partial[subbed] {
    subbed := 7 - 3
}

infix_coverage := wassup {
    1 == 1
    1 != 2
    2 >= 1
    1 <= 1
    1 < 2
    2 > 1
    1 * 2
    1 / 2
    "a" == "a"
    "a" != "b"
    wassup := "wassup"
}
modulo := 20.0 % 7
bad_modulo := 20.5 % 7

infix_lies {
    [
        1 == 2,
        1 != 1,
        1 >= 2,
        2 <= 1,
        2 < 1,
        1 > 2,
        "a" == "b",
        "a" != "a",
        "a" + "b",
        "a" == 1,
        1 == "a",
        ["hello", "world"] = ["hello", "world", 6],
        "hello" = ["hello"],
        ["hello"] = "hello"
    ][true]
}

op1 := 4 / 2 / 2
op15 := 4 / 2 * 2
op16 := 4 * 2 / 4
op2 := 4 / 2 + 2
op3 := 2 + 4 / 2
op35 := (2 + 4) / 2
op4 := 2 - 4 * 2
op45 := (2 - 4) * 2
op5 := 2 - 4 * 2 + 3
op52 := 2 - 4 - 6 * 3 + 2
op53 := 2 - 4 * 2 - 6 * 3 + 2
op55 := 2 - 4 * (2 + 3)
op56 := (2 - 4) * (2 + 3)
op6 := 2 - 5 % 3 + 3
op65 := (2 - 5) % 3 + 3
op66 := (2 - 5) % (3 + 3)
op7 := 3 + 3 > 4 + 1
op75 := 4 != 3 == 4 >= 3 != 4 <= 3
op76 := 4 != 4 == 4 >= 5 != 4 <= 5
op8 := 1 + 2 != 2 + 3 == false
op85 := 9 <= 3 * 1 + 2 * 5 == 4 >= 3
op86 := 9 <= 3 * (1 + 2) * 5 == 4 >= 3
op9 := {1, 2} | {2, 3}
op10 := {1, 2} & {2, 3}
op11 := {1, 2} & {2, 3} | {1, 2} & {1, 3}

matching_sets := {1, 2, 3} == {3, 1, 2}
unmatched_sets := {1, 2, 3} == {3, 1, 4}
matching_sets_r { {1, 2, 3} == {3, 1, 2} }
unmatched_sets_r { {1, 2, 3} == {3, 1, 4} }

comprehension_data := ["a", "b", "a"]
array_comprehension := [val | val := comprehension_data[_]]
set_comprehension := {val | val := comprehension_data[_]}
object_comprehension := {val: val | val := comprehension_data[_]}

unification_1 := x {
    [x, "world"] = ["hello", y]
    x = x
}

unification_2 := x {
    ["hello", "world", "hello"][x] = "hello"
    [1, 2, 3][x] = 3
}

unification_3[x] {
    x = ["hello", "world"][_]
}

unification_4[x] = [1, 2, 3, 4][x] < 3

unification_5 := x {
    [x, "world"] = ["hello", "earth"]
}

userFunc1Result := UserFunction1()
UserFunction1() = retVal {
    retVal := "userFunction1 executed"
}

userFunc2Result := UserFunction2("doubleTrouble")
UserFunction2(arg) = retVal {
    retVal := data[arg]
}

duplicate_function(a) = r {
   a < 0
   r := a
}

duplicate_function(a) = r {
   a > 0
   r := a + 1
}

duplicate_function_l := duplicate_function(-1)
duplicate_function_l := duplicate_function(0)
duplicate_function_t := duplicate_function(1)
duplicate_function_t := duplicate_function(0)

userFuncDefaultVal(query) {
    query == 4
}
userFuncDefaultVal_match := userFuncDefaultVal(4)
userFuncDefaultVal_no_match := userFuncDefaultVal(5)

undefinedResponse {
    "undefined"
}

rule_body_good_loops {
    c := input.rule_body_loops_c
    b := data.rule_body_loops_b
    a := b[c]
    a < 7
}

rule_body_bad_loops {
    c := input.rule_body_loops_c
    b := data.rule_body_loops_b
    a := b[c]
    a > 7
}

rule_body_bad_ref {
    c := input.rule_body_loops_lol
    b := data.rule_body_loops_b
    a := b[c]
    a > 7
}

rule_body_unnamed_false {
    7 < 5
}

rule_body_named_false {
    a := 7 < 5
}

array_ref := data_arr {
    data_arr1 := data.array[1]
    data_arr_ := data.array[0]
    data_arr := data_arr1 + data_arr_
}

empty_array_ref[file] {
    false
    file := "not reachable line"
}

empty_array_ref_reader[output] {
    output := empty_array_ref[_]
}

array_ref_u := data.array[2]

file_reader[file] {
    file := data.files[_]
}

byte_reader[byte] {
    file_name := data.files[_]
    file := data.multi_files[_]
    byte := file[file_name][_]
}

files_below_400[file.name] {
    file := data.file_sizes[_]
    file.size < 400
}

files_above_400[file.name] {
    file := data.file_sizes[_]
    file.size > 400
}

file_counts_as_expected {
    count(files_below_400) <= 3
    count(files_above_400) >= 3
    count(data.file_sizes) == 7
}

# Math functions
numop_nearest_1 := round(76.8)
numop_nearest_2 := round(77.3)
numop_nearest_3 := round(-76.8)
numop_nearest_4 := round(-77.3)
numop_ceil_1 := ceil(76.8)
numop_ceil_2 := ceil(76.3)
numop_ceil_3 := ceil(-76.8)
numop_ceil_4 := ceil(-76.3)
numop_floor_1 := floor(76.8)
numop_floor_2 := floor(76.3)
numop_floor_3 := floor(-76.8)
numop_floor_4 := floor(-76.3)
numop_abs_1 := abs(-5.2)
numop_abs_2 := abs(5.2)

# range function
nine_twenty_double := numbers.range(8.3, 20.8)
twenty_nine_double := numbers.range(20.9, 8.4)
single_range := numbers.range(14.5, 15.5)

# aggregate functions
summer := sum([1, 2, 3])
winter := product([1, 2, 3])
maxer := max([2, 1, 3])
miner := min([2, 1, 3])
mixed_agg := sum([1, product([1, 2, max([2, min([1, count("thr")])])]), 5])
strlen := count("fives")
bad_type_agg := product(["hello"])
good_count := count([1, 2, 4])
empty_sum := sum([])
empty_product := product([])
empty_count := count([])
empty_max := max([])
empty_min := min([])

allTrue := all([true, true, true])
fewTrue := all([1, true, "hello"])
noneTrue := any([false, 5, "world"])
oneTrue := any([3, "what", true])
emptyAll := all([])
emptyAny := any([])
mixAllAny := all([true, any([false, all([])])])

array_cat := array.concat([1, true], ["hello", [false]])
array_empty_cat := array.concat([], [])
array_ice := array.slice(array_cat, 1, count(array_cat) - 1)
array_bad_ice_1 := array.slice(array_cat, 0, 100)
array_bad_ice_5 := array.slice(array_cat, 2, 2)

# object functions
obj_key_found := object.get({"k": "v"}, "k", "dv")
obj_key_not_found := object.get({}, "k", "dv")

set1 := {1, 2, 3}
set2 := {3, 4, 5}
seti := set1 & set2
setu := set1 | set2
setd := set1 - set2
setI := intersection({set1, set2, seti})
setU := union({set1, set2, setu})
setC {
    {1, 2, 3} == {3, 2, 1}
}

else_first = "first" {
    true
} else = "default"

else_second = "first" {
    false
} else = "second" {
    true
} else = "default"

else_third = "first" {
    false
} else = "second" {
    false
} else = "third" {
    true
} else = "default"

else_default = "first" {
    false
} else = "second" {
    false
} else = "third" {
    false
} else = "default"

user_function_else(key) = value {
    value := data.key_value_pair[key]
} else = "nothing"
user_function_else_found := user_function_else("found")
user_function_else_not_found := user_function_else("not found")

user_func_multi_else(in) = in {
    false
} else = in {
    in == "e1"
} else = in {
    in == "e2"
} else = "whatever"
user_func_multi_else_e1 = user_func_multi_else("e1")
user_func_multi_else_e2 = user_func_multi_else("e2")
user_func_multi_else_default = user_func_multi_else("lol")

user_func_no_default(in) = in {
    in == "first"
} else = in {
    in == "second"
}
user_func_no_default_first = user_func_no_default("first")
user_func_no_default_second = user_func_no_default("second")
user_func_no_default_unknown = user_func_no_default("unknown")

conflict_rule[out] {
    true
    out := "done"
}

conflict_rule[out] {
    false
    out := "not done"
}

some_1[out] {
    some x
    "a1" == data.some_array[x]
    out := data.some_array[x]
}

some_2[out] {
    some x, y
    data.some_array[x] == data.some_array2[y]
    out := [x, y]
}

# Policy source: https://www.openpolicyagent.org/docs/latest/policy-language/#some-keyword
tuples[[i, j]] {
    some i, j
    data.sites[i].region == "west"
    server := data.sites[i].servers[j]
    contains(server.name, "db")
}

some_1_implicit[out] {
    "a1" == data.some_array[x]
    out := data.some_array[x]
}

some_2_implicit[out] {
    data.some_array[x] == data.some_array2[y]
    out := [x, y]
}

tuples_implicit[[i, j]] {
    data.sites[i].region == "west"
    server := data.sites[i].servers[j]
    contains(server.name, "db")
}

# string functions
concat_test := concat(", ", ["a", true, false, 7, 0.8, [1, "2"]])
concat_empty_test := concat(", ", [])
concat_one_test := concat(", ", ["a"])

contains_true := contains("abcde", "bc")
contains_false := contains("abcde", "cb")

endswith_true := endswith("abcde", "de")
endswith_false := endswith("abcde", "cd")

format_int_binary := format_int(8, 2)
format_int_octal := format_int(8.2, 8)
format_int_decimal := format_int(8.9, 10)
format_int_hex := format_int(-82.9, 16)
format_int_unknown := format_int(8, 20)
format_int_large := format_int(10223372036854775807, 16)

indexof_found := indexof("abcabcabc", "ca")
indexof_not_found := indexof("abcabcabc", "ba")

lowered := lower("aB7cD3")

startswith_true := startswith("abcde", "ab")
startswith_false := startswith("abcde", "b")

upperred := upper("aB7cD3")

substring_empty := substring("abcd:1234", 20, 1)
substring_1234_1 := substring("abcd:1234", 5, -3)
substring_1234_2 := substring("abcd:1234", 5, 20)
substring_abcd := substring("abcd:1234", 0, 4)
substring_unknown := substring("abcd:1234", -5, 3)