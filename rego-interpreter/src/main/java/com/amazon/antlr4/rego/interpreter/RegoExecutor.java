// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoLexer;
import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.interpreter.core.PolicyVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.lib.PackageNameVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.DecoratedVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.RegoBreakpointVisitor;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.VisitCoverage;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.VisitTracer;
import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.VisitDecorator;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;
import com.amazon.antlr4.rego.interpreter.type.Visitation;

import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.tree.ParseTree;

import lombok.extern.log4j.Log4j2;

/**
 * Execute visitor against Rego policy.
 *
 * <p>This class implements the
 * <a href="https://en.wikipedia.org/wiki/Facade_pattern">Facade</a> design pattern.
 * It provides an interface to the underlying Rego
 * <a href="https://en.wikipedia.org/wiki/Interpreter_pattern">Interpreter</a> implementation.
 * This is the only class that customers of this package should interact with.
 */
@Log4j2
@SkipMethodTrace
public final class RegoExecutor {
    private final ParseTree policyTree;
    private final JsonObject data;
    private final Map<String, BaseExpression> exprMap;
    private final Map<String, Object> initialContextMap;
    private final boolean withKeywordEnabled;
    private final boolean strictTypeCheckEnabled;
    private final RegoExecutor dependent;
    private final List<VisitDecorator> decorators;
    private final String packageName;

    RegoExecutor(RegoExecutorBuilder builder) throws IOException, BadPolicyException {
        try {
            data = loadJSON(builder.data());
            policyTree = prepPolicyTree(CharStreams.fromStream(builder.policy()));
            packageName = buildPackageName();
            exprMap = builder.expressions();
            initialContextMap = builder.initialContextMap();
            withKeywordEnabled = builder.withKeyword();
            strictTypeCheckEnabled = builder.strictTypeCheck();
            dependent = buildDependent(builder);
            decorators = buildDecorators(builder);
        } catch (ParseCancellationException e) {
            throw new BadPolicyException(e);
        }
    }

    public String getPackageName() {
        return packageName;
    }

    public Map<String, Visitation> getCoverageMap() {
        List<VisitDecorator> coverageDecorators = decorators.stream()
            .filter(d -> d instanceof VisitCoverage)
            .collect(Collectors.toList());

        if (coverageDecorators.size() == 0) {
            throw new RuntimeException("Coverage is not enabled");
        }
        VisitCoverage coverageDecorator = (VisitCoverage) decorators.get(0);

        return coverageDecorator.getCoverageMap();
    }

    public JsonObject executePolicy(InputStream input) throws IOException, BadPolicyException {
        return executePolicy(loadJSON(input));
    }

    public JsonObject executePolicy(JsonObject input) throws IOException, BadPolicyException {
        return executePolicy(input, null, null);
    }

    public JsonObject executePolicy(JsonObject input, String parentPackage, JsonObject parentOutput)
            throws IOException, BadPolicyException {

        // PolicyVisitor clones the context map on creation, so the initial map won't be modified over time.
        PolicyVisitor processor = null;
        try {
            processor = newPolicyVisitor(input, parentPackage, parentOutput)
                .withKeywordEnabled(withKeywordEnabled)
                .withStrictTypeCheckEnabled(strictTypeCheckEnabled)
                .withJavaFunctions(exprMap);
            processor.visit(policyTree);
            decorators.forEach(d -> d.endVisit(policyTree));
            if (dependent != null) {
                JsonObject output = JsonResolver.extractOutput(processor.getRuleManager(), true);
                return dependent.executePolicy(input, getPackageName(), output);
            }
            return JsonResolver.extractOutput(processor.getRuleManager(), false);
        } catch (RegoProcessorException e) {
            throw new BadPolicyException(e);
        } finally {
            if (processor != null) {
                processor.cleanupExpressions();
            }
        }
    }

    private PolicyVisitor newPolicyVisitor(JsonObject input, String parentPackage, JsonObject parentOutput) {
        if (decorators.size() == 0) {
            return new PolicyVisitor(initialContextMap, input, overrideData(parentPackage, parentOutput));
        }
        return new DecoratedVisitor(
            initialContextMap, input, overrideData(parentPackage, parentOutput)
            ).withDecorators(decorators);
    }

    private JsonObject overrideData(String parentPackage, JsonObject parentOutput) {
        if (parentOutput == null) {
            return data;
        }
        String[] overrideKeys = parentPackage.split("\\.");
        return recursiveMerge(data, overrideKeys, 0, parentOutput).build();
    }

    private final JsonBuilderFactory jsonBuilderFactory = Json.createBuilderFactory(null);

    private JsonObjectBuilder recursiveMerge(JsonObject original, String[] keys, int keyIndex, JsonValue override) {
        final String key = keys[keyIndex];
        JsonObjectBuilder objectBuilder = jsonBuilderFactory.createObjectBuilder();

        // add non overlaps
        original.entrySet().stream()
            .filter(es -> !es.getKey().equals(key))
            .forEach(es -> objectBuilder.add(es.getKey(), es.getValue()));

        if (keyIndex + 1 == keys.length) { // end of line
            objectBuilder.add(key, override);
        } else if (!original.containsKey(key)) { // no overlap, recurse keys
            objectBuilder.add(key, recursiveBuild(keys, keyIndex + 1, override));
        } else if (!(original.get(key) instanceof JsonObject)) { // override missing object
            objectBuilder.add(key, recursiveBuild(keys, keyIndex + 1, override));
        } else {
            objectBuilder.add(key, recursiveMerge(original.getJsonObject(key), keys, keyIndex + 1, override));
        }
        return objectBuilder;
    }

    private JsonObjectBuilder recursiveBuild(String[] keys, int keyIndex, JsonValue override) {
        JsonObjectBuilder objectBuilder = jsonBuilderFactory.createObjectBuilder();
        if (keyIndex + 1 == keys.length) {
            objectBuilder.add(keys[keyIndex], override);
        } else {
            objectBuilder.add(keys[keyIndex], recursiveBuild(keys, keyIndex + 1, override));
        }
        return objectBuilder;
    }

    public JsonObject executePolicy(String input) throws IOException, BadPolicyException {
        return executePolicy(RegoExecutorBuilder.stringToInputStream(input));
    }

    static ParseTree prepPolicyTree(CharStream policy) {
        RegoLexer lexer = new RegoLexer(policy);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RegoParser parser = new RegoParser(tokens);
        parser.setErrorHandler(new BailErrorStrategy());
        return parser.module();
    }

    private String buildPackageName() {
        return ((JsonString) new PackageNameVisitor().visit(policyTree)).getString();
    }

    private static RegoExecutor buildDependent(RegoExecutorBuilder builder) throws IOException, BadPolicyException {
        if (builder.dependentBuilder() != null) {
            return new RegoExecutor(builder.dependentBuilder());
        }
        return null;
    }

    private List<VisitDecorator> buildDecorators(RegoExecutorBuilder builder) throws IOException, BadPolicyException {
        List<VisitDecorator> decorators = new ArrayList<>(2);
        if (builder.coverage()) {
            decorators.add(new VisitCoverage());
        }
        if (log.isTraceEnabled()) {
            decorators.add(new VisitTracer());
        }
        if (!builder.regoBreakpoints().isEmpty()) {
            decorators.add(new RegoBreakpointVisitor(builder.regoBreakpoints()));
        }
        return decorators;
    }

    public static JsonObject loadJSON(InputStream is) {
        JsonReader reader = Json.createReader(is);
        return reader.readObject();
    }
}
