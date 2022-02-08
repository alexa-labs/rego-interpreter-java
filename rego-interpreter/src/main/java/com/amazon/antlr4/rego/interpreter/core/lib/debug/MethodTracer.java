// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.log4j.Log4j2;

/**
 * Set using <code>-DrequireMethodTrace=true</code>
 * (on command line) to enable method tracing.
 */
@Aspect
@Log4j2
public class MethodTracer {

    @Pointcut("execution(* *(..))")
    public void methods() {}

    private int callDepth = 0;
    private String lastEntry = null;
    private boolean deepSkipEnabled = false;

    /**
     * Skip tracing of this method or all methods in this class.
     * Any methods invoked from here might be traced.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public static @interface SkipMethodTrace {}

    /**
     * Skip tracing of this method or all methods in this class.
     * All tracing is skipped till this method completes.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.TYPE})
    public static @interface SkipDeepMethodTrace {}

    private static final String POSSIBLE_LAMBDA_PREFIX = "lambda$";

    @Around("com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.methods()")
    @SuppressFBWarnings("ES_COMPARING_STRINGS_WITH_EQ")
    public Object traceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!log.isTraceEnabled() || deepSkipEnabled) {
            return joinPoint.proceed();
        }

        Class<?> type = joinPoint.getStaticPart().getSignature().getDeclaringType();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        if (type.getAnnotation(SkipDeepMethodTrace.class) != null
            || method.getAnnotation(SkipDeepMethodTrace.class) != null) {

            deepSkipEnabled = true;
            try {
                return joinPoint.proceed();
            } finally {
                deepSkipEnabled = false;
            }
        }

        if (type.getAnnotation(SkipMethodTrace.class) != null) {
            return joinPoint.proceed();
        }

        if (method.getAnnotation(SkipMethodTrace.class) != null
            || method.getName().startsWith(POSSIBLE_LAMBDA_PREFIX)) {
            return joinPoint.proceed();
        }

        final String loc = type.getSimpleName() + "::" + method.getName();
        final String indent = String.format("%" + (callDepth + 1) + "s", "");
        final String thisEntry = String.format("%s Calling %s", indent, loc);
        lastEntry = thisEntry;

        try {
            callDepth++;
            System.out.println(thisEntry);
            return joinPoint.proceed();
        } finally {
            callDepth--;
            if (lastEntry != thisEntry) {
                System.out.println(String.format("%s Completed %s", indent, loc));
            }
        }
    }
}
