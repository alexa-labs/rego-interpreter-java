// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.type;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.interpreter.core.PolicyVisitor;
import com.amazon.antlr4.rego.interpreter.core.RuleManager;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;

/**
 * Rules that work like unnamed local variables which are automagically assigned.
 * See OPA documentation
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#variable-keys">here</a>.
 * <p>
 * <strong>Implementation Notes</strong>
 * <p>
 * Create jobs for underscore values. Implementation is spread across four main classes (in order of appearance):
 * <ul>
 * <li>{@link ScopedRuleRunner} - when running rules in local scope, registers an underscore object to the symbol table.
 * After first execution, creates more loops if underscore object reports additional jobs are available.
 * Each job is executed in its <b>own scope</b> with the job registered to the new local scope.
 * <li>{@link PolicyVisitor} - when resolving references, will detect that underscore is in use.
 * It will get underscore object from rule manager (there is only one; registered by ScopedRuleRunner).
 * The rule context and array reference will be passed to underscore object to <code>peek</code> at current value.
 * <li>{@link RuleManager} - provides underscore object from current symbol table using static constant key.
 * <li>{@link UnderscoreLocation}
 * <ul>
 * <li>Each instance of this class represents one unique permutation of underscore values in scope.
 * <code>peek</code> generates underscore permutations using DFS.
 * When a new underscore is found, its first value is added to current instance.
 * Remaining values are added to copies and stored in a job queue.
 * <li>All underscore values are stored in a list. So, the copy constructor can generate the next permutation.
 * <li>All underscore occurances are stored in a map. So, <code>peek</code> knows if it encounterd a new underscore.
 * <li>Maintains a job queue populated on underscore discovery so <b>all</b> loop iterations can be run in parallel.
 * </ul>
 * </ul>
 *
 * <p>This class is an extreme implementation of the
 * <a href="https://en.wikipedia.org/wiki/Adapter_pattern">Adapter</a> design pattern.
 * It masquerades as a {@link Rule} Target for the {@link RuleManager} Client.
 * It also masquerades as a {@link JsonValue} Target for the {@link PolicyVisitor} Client.
 * In both cases, the Adaptee is Rego's
 * <a href="https://www.openpolicyagent.org/docs/latest/policy-language/#variable-keys">underscore variable keys</a>.
 *
 * <p>Note that no methods are adapted. Only the type is adapted to allow storing and passing the object.
 */
public class UnderscoreLocation extends Rule implements JsonValue {

    public static final String UNDERSCORE_RULE_KEY = UUID.randomUUID().toString();

    /**
     * Immutable reference to a single value of an underscore variable.
     */
    private static class UnderscoreValue {
        private final JsonArray array;
        private final UnderscoreValue next;
        private int index;

        /**
         * Reference to first value of the underscore variable.
         */
        UnderscoreValue(JsonArray array, UnderscoreValue next) {
            this.array = array;
            this.next = next;
            this.index = 0;
        }

        JsonValue peek() {
            if (array.isEmpty()) {
                throw new RegoProcessorException.BadReferenceException("Read from empty array");
            }
            return array.get(index);
        }

        boolean areReadyForNext() {
            return next.areReadyForNext() || this.isReadyForNext();
        }

        private boolean isReadyForNext() {
            if (index + 1 < array.size()) {
                index++;
                next.reset();
                return true;
            }
            return false;
        }

        protected void reset() {
            index = 0;
            next.reset();
        }
    }

    private UnderscoreValue lastUnderscore = new UnderscoreValue(null, null) {
        @Override
        boolean areReadyForNext() {
            return false;
        }

        @Override
        protected void reset() {}
    };

    /**
     * Provide search for the underscore runtime context.
     */
    private final Map<String, UnderscoreValue> arrayMap;

    /**
     * Create the root object for the local scope before first run.
     */
    public UnderscoreLocation() {
        super(UNDERSCORE_RULE_KEY, null);
        arrayMap = new HashMap<>();
    }

    public JsonValue peek(Object ctx, JsonArray state) {
        String key = ctx.hashCode() + "-" + state.hashCode();
        UnderscoreValue score = arrayMap.get(key);
        if (score == null) {
            lastUnderscore = new UnderscoreValue(state, lastUnderscore);
            score = lastUnderscore;
            arrayMap.put(key, score);
        }
        return score.peek();
    }

    public JsonValue peekIndex(Object ctx, JsonArray state) {
        String key = ctx.hashCode() + "-" + state.hashCode();
        UnderscoreValue score = arrayMap.get(key);
        if (score == null) {
            return JsonValue.NULL;
        }
        return JsonResolver.nativeToJson(score.index);
    }

    public synchronized boolean isReadyForNext() {
        return lastUnderscore != null && lastUnderscore.areReadyForNext();
    }

    /**
     * We don't want JSON tools to process this type because it doens't have a static value.
     */
    @Override
    public ValueType getValueType() {
        throw new RegoProcessorException.BadReferenceException("UnderscoreLocation isn't valid JsonValue");
    }

    @Override
    public void setValue(JsonValue... values) {
        throw new RuntimeException("cannot set values on underscore");
    }

    @Override
    public JsonValue getValue() {
        return this;
    }
}
