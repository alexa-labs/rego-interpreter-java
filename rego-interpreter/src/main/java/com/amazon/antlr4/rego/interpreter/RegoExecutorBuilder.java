// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;
import com.amazon.antlr4.rego.interpreter.type.BadPolicyException;
import com.amazon.antlr4.rego.interpreter.type.ExpressionArgument;

import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SkipMethodTrace
public class RegoExecutorBuilder {

    private final InputStream policy;
    private InputStream data = stringToInputStream("{}");
    private Map<String, BaseExpression> expressions = new HashMap<>();
    private Map<String, Object> initialContextMap = new HashMap<>();
    private boolean withKeyword = false;
    private boolean strictTypeCheck = false;
    private boolean coverage = false;
    private RegoExecutorBuilder dependentBuilder = null;
    private Set<String> regoBreakpoints = new HashSet<>();

    public RegoExecutorBuilder(InputStream policy) {
        confirmLogLevel();
        this.policy = policy;
    }

    public RegoExecutorBuilder(String policy) {
        confirmLogLevel();
        this.policy = stringToInputStream(policy);
    }

    public RegoExecutorBuilder data(InputStream data) {
        this.data = data;
        return this;
    }

    public RegoExecutorBuilder data(String data) {
        this.data = stringToInputStream(data);
        return this;
    }

    public RegoExecutorBuilder initialContextMap(Map<String, Object> initialContextMap) {
        if (initialContextMap != null) {
            this.initialContextMap = initialContextMap;
        } else {
            this.initialContextMap = new HashMap<>();
        }
        return this;
    }

    /**
     * After this policy is executed, the output is merged with data of dependent policy.
     * The dependent policy is then executed and it's output returned.
     * <p/>
     * If this package is <code>a.b</code>, then its output <code>c</code> will be available to
     * dependent as <code>data.a.b.c</code>.
     */
    public RegoExecutorBuilder dependentBuilder(RegoExecutorBuilder dependentBuilder) {
        this.dependentBuilder = dependentBuilder;
        return this;
    }

    public <T extends BaseExpression> RegoExecutorBuilder expressions(Map<String, T> expressions) {
        copyExpressionMap(expressions, this.expressions);
        return this;
    }

    /**
     * Allows using the *with* keyword in Rego policy.
     * It is disabled by default and will cause an exception if used.
     */
    public RegoExecutorBuilder withKeyword(boolean withKeyword) {
        this.withKeyword = withKeyword;
        return this;
    }

    /**
     * Type check will be done for any native functions that are annotated with {@link ExpressionArgument}.
     * There is a performance cost to this type check. Hence, it is disabled by default.
     */
    public RegoExecutorBuilder strictTypeCheck(boolean strictTypeCheck) {
        this.strictTypeCheck = strictTypeCheck;
        return this;
    }

    /**
     * Indicate breakpoints in Rego policy using line numer and context type.
     * These breakpoints will be hit in
     * {@link com.amazon.antlr4.rego.interpreter.core.lib.debug.RegoBreakpointVisitor RegoBreakpointVisitor}.
     * Setup your debugger to stop there. To find possible breakpoints, render policy or enable trace logs.
     */
    public RegoExecutorBuilder regoBreakpoint(int lineNumber, Class<? extends ParserRuleContext> contextType) {
        if (!testMode) {
            throw new SecurityException("Breakpoints require test mode");
        } else if (!log.isDebugEnabled()) {
            throw new RuntimeException("Breakpoints require debug logs enabled");
        }
        this.regoBreakpoints.add(lineNumber + contextType.getName());
        return this;
    }

    public RegoExecutorBuilder coverage(boolean coverage) {
        if (coverage && !testMode) {
            throw new SecurityException("Coverage requires test mode");
        } else if (coverage && !log.isDebugEnabled()) {
            throw new RuntimeException("Coverage requires debug logs enabled");
        }
        this.coverage = coverage;
        return this;
    }

    public RegoExecutor build() throws IOException, BadPolicyException {
        return new RegoExecutor(this);
    }

    InputStream policy() {
        return policy;
    }

    InputStream data() {
        return data;
    }

    RegoExecutorBuilder dependentBuilder() {
        return dependentBuilder;
    }

    Map<String, BaseExpression> expressions() {
        return expressions;
    }

    Map<String, Object> initialContextMap() {
        return initialContextMap;
    }

    boolean withKeyword() {
        return withKeyword;
    }

    Set<String> regoBreakpoints() {
        return regoBreakpoints;
    }

    private static <T extends BaseExpression>
    void copyExpressionMap(Map<String, T> src, Map<String, BaseExpression> dst) {
        src.entrySet().stream().forEach(es -> dst.put(es.getKey(), es.getValue()));
    }

    static InputStream stringToInputStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean testMode = false;

    public static void setTestMode() {
        testMode = true;
    }

    public static void setTestMode(Level level) {
        setTestMode();
        setLogLevel(level);
    }

    public static void setLogLevel(Level level) {
        Configurator.setRootLevel(level);
    }

    private static void confirmLogLevel() {
        if (!testMode) {
            Logger log = LogManager.getRootLogger();
            if (log.getLevel().isLessSpecificThan(Level.DEBUG)) {
                throw new SecurityException("Debug or trace logs are only safe in test mode");
            }
        }
    }

    boolean strictTypeCheck() {
        return strictTypeCheck;
    }

    boolean coverage() {
        return coverage;
    }
}
