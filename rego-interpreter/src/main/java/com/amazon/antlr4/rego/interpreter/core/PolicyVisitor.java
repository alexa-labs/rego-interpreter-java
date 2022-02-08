// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoBaseVisitor;
import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.RegoParser.Ref_argContext;
import com.amazon.antlr4.rego.RegoParser.Rule_bodyContext;
import com.amazon.antlr4.rego.RegoParser.Rule_definitionContext;
import com.amazon.antlr4.rego.interpreter.core.lib.ExprLibrary;
import com.amazon.antlr4.rego.interpreter.core.lib.InfixOperator;
import com.amazon.antlr4.rego.interpreter.core.lib.JsonResolver;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.BaseExpression;
import com.amazon.antlr4.rego.interpreter.core.type.CompleteRule;
import com.amazon.antlr4.rego.interpreter.core.type.IndexAssignment;
import com.amazon.antlr4.rego.interpreter.core.type.ObjectRule;
import com.amazon.antlr4.rego.interpreter.core.type.OperatorType;
import com.amazon.antlr4.rego.interpreter.core.type.RegoProcessorException;
import com.amazon.antlr4.rego.interpreter.core.type.Rule;
import com.amazon.antlr4.rego.interpreter.core.type.RuleFactory;
import com.amazon.antlr4.rego.interpreter.core.type.SetRule;
import com.amazon.antlr4.rego.interpreter.core.type.SomeRule;
import com.amazon.antlr4.rego.interpreter.core.type.UnderscoreLocation;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Visitor for ANTLR4 tree generated for Rego DSL.
 *
 * <p>This class is a Concrete Visitor in the
 * <a href="https://en.wikipedia.org/wiki/Visitor_pattern">Visitor</a> design pattern.
 * It receives a parse tree object representing the specific Rego policy.
 * The parse tree is constructed by ANTLR4 to adhere to the
 * <a href="https://code.amazon.com/packages/RegoGrammar/blobs/mainline/--/grammar/Rego.g4">Rego DSL</a>.
 *
 * <p>We use {@link JsonValue} as the return type and also as the type for all objects passed around in the policy.
 * This works well with <a href="https://play.openpolicyagent.org/">Rego's</a>
 * input, data, and output variables which are all of type {@link JsonObject}.
 * However, there is possible loss of fidelity since there is no JsonSet type
 * and {@link JsonNumber} doesn't retain user intent (double? int? long?).
 *
 * <p>Both {@link ParseTree} and {@link JsonValue} are
 * <a href ="https://en.wikipedia.org/wiki/Composite_pattern">Composite</a>
 * objects that allow uniform treatment of the policy and objects created by it respectively.
 */
public class PolicyVisitor extends RegoBaseVisitor<JsonValue> {

    private final RuleManager ruleManager;
    private final RuleRunner ruleRunner;
    private final ExprLibrary exprLibrary;
    private boolean withKeywordEnabled = false;

    /**
     * By adding input and data to the RuleManager.globalRuleFrame we avoid writing
     * special logic. This needs to be done before any RuleFrames are added or the
     * RuleStack is copied.
     */
    public PolicyVisitor(Map<String, Object> initialContextMap, JsonObject input, JsonObject data) {
        ruleManager = new RuleManager();
        ruleRunner = new RuleRunner(this, ruleManager);
        ruleManager.putRule(new CompleteRule("input", null, input));
        ruleManager.putRule(new CompleteRule("data", null, data));
        exprLibrary = new ExprLibrary(initialContextMap, input, data);
    }

    public PolicyVisitor(PolicyVisitor visitor, RuleManager ruleManager) {
        this.ruleManager = ruleManager;
        ruleRunner = new RuleRunner(this, ruleManager);
        exprLibrary = new ExprLibrary(
            visitor.exprLibrary.userContext,
            (JsonObject) ruleManager.getRuleValue("input"),
            (JsonObject) ruleManager.getRuleValue("data")
        );
        exprLibrary.strictTypeCheckEnabled(visitor.exprLibrary.strictTypeCheckEnabled());
        withKeywordEnabled = true;
    }

    @Override
    @SkipMethodTrace
    public JsonValue visit(ParseTree tree) {
        if (tree == null) {
            return null;
        }
        return super.visit(tree);
    }

    @Override
    public JsonValue visitRimport(RegoParser.RimportContext ctx) {
        JsonValue value = visit(ctx.ref());
        String name = getImportName(ctx);
        Rule rule = new CompleteRule(name, null, value);
        ruleManager.putRule(rule);
        return null;
    }

    private String getImportName(RegoParser.RimportContext ctx) {
        if (ctx.VAR() != null) {
            return ctx.VAR().getText();
        }
        List<Ref_argContext> refArg = ctx.ref().ref_arg();
        int refArgSize = refArg.size();
        return refArg.get(refArgSize - 1).VAR().getText();
    }

    /**
     * Create list of Policy.RRule[] and execute in parallel.
     */
    @Override
    public JsonValue visitPolicy(RegoParser.PolicyContext ctx) {
        ruleRunner.runAllRules(RuleFactory.buildFromPolicy(ctx), false);
        return null;
    }

    /**
     * Create list of Query.Literal[] and execute in parallel.
     */
    @Override
    public JsonValue visitQuery(RegoParser.QueryContext ctx) {
        ruleRunner.runAllRules(RuleFactory.buildFromQuery(ctx), true);
        return null;
    }

    /**
     * A rule body requires its own scope to define local variables. The rule
     * definition will ask rule runner to set up the new scope. The older scope
     * variables are still visible to read (when not overridden). Only the rule's
     * final computed value is added to the current scope. Everything else is lost.
     */
    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitRule_definition(Rule_definitionContext ctx) {
        if (ctx.DEFAULT() != null) {
            return processDefaultRule(ctx);
        }
        RegoParser.Rule_headContext hctx = ctx.rule_head();
        boolean partial = hctx.rule_index() != null;
        executeRuleBody(ctx.rule_body(), getRuleFromDefinitionContext(ctx), findRuleAssignmentContext(hctx), partial);
        return null;
    }

    private void executeRuleBody(Rule_bodyContext bctx, Rule destRule, IndexAssignment<ParseTree> ia, boolean partial) {
        try {
            ruleRunner.executeRuleContext(
                Collections.emptyList(), bctx,
                destRule, ia, partial
            );
        } catch (RegoProcessorException.RuleEvaluationException e) {
            if (bctx != null && (bctx.iterm() != null || bctx.rule_body() != null)) {
                executeRuleBody(
                    bctx.rule_body(), destRule,
                    new IndexAssignment<>(ia.indexContext, bctx.iterm()), partial
                );
                return;
            }
            throw e;
        }
    }

    public IndexAssignment<JsonValue> resolveIndexAssignment(IndexAssignment<ParseTree> ia) {
        JsonValue value = visit(ia.assignContext);
        JsonValue index = visit(ia.indexContext);
        return new IndexAssignment<>(index, value);
    }

    private Rule getRuleFromDefinitionContext(Rule_definitionContext ctx) {
        String ruleName = ctx.rule_head().VAR().getText();
        return ruleManager.getRule(ctx.getParent(), ruleName);
    }

    private IndexAssignment<ParseTree> findRuleAssignmentContext(RegoParser.Rule_headContext hctx) {
        RegoParser.ItermContext ictx = null;
        if (hctx.rule_index() != null) {
            ictx = hctx.rule_index().iterm();
        }
        RegoParser.ItermContext actx = null;
        if (hctx.rule_assignment() != null) {
            actx = hctx.rule_assignment().iterm();
        }
        return new IndexAssignment<>(ictx, actx);
    }

    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitRule_body(Rule_bodyContext ctx) {
        validateElseNotAssigned(ctx);
        return visit(ctx.query());
    }

    private void validateElseNotAssigned(Rule_bodyContext ctx) {
        if (ctx.getParent() instanceof Rule_definitionContext && ctx.rule_body() != null) {
            Rule_definitionContext ruleDef = (Rule_definitionContext) ctx.getParent();
            if (ruleDef.rule_head().getChild(1).getText().equals(":=")) {
                throw new RegoProcessorException.UncheckedBadPolicyException(
                    "Cannot mix rule assignment (:= operator) with else keyword");
            }
        }
    }

    private JsonValue processDefaultRule(Rule_definitionContext ctx) {
        CompleteRule rule = getOrPutRule(ctx);
        rule.setDefaultValue(visit(ctx.scalar()));
        return null;
    }

    private CompleteRule getOrPutRule(Rule_definitionContext ctx) {
        String ruleName = ctx.VAR().getText();
        try {
            ruleManager.getRule(ctx.getParent(), ruleName);
            throw new RegoProcessorException.UncheckedBadPolicyException(
                    String.format("rego_type_error: multiple default rules named `%s` found", ruleName));
        } catch (RegoProcessorException.UndefinedRuleException e) {
            CompleteRule rule = new CompleteRule(ruleName, ctx);
            ruleManager.putRule(rule);
            return rule;
        }
    }

    @Override
    public JsonValue visitIterm(RegoParser.ItermContext ctx) {
        return ItermVisitor.visit(this, ctx);
    }

    @Override
    public JsonValue visitStat(RegoParser.StatContext ctx) {
        RegoParser.LiteralContext lctx = (RegoParser.LiteralContext) ctx.parent;
        JsonValue statValue = JsonValue.FALSE;
        try {
            statValue = executeStat(ctx, lctx);
        } catch (RegoProcessorException.UndefinedRuleException | RegoProcessorException.BadReferenceException e) {
            if (!hasNot(ctx)) {
                throw e;
            }
        }
        ruleManager.getRule(ctx).setValue(visitStatNOT(ctx, statValue));
        return null;
    }

    private JsonValue executeStat(RegoParser.StatContext ctx, RegoParser.LiteralContext lctx) {
        if (lctx.with_modifier().size() > 0) {
            if (!withKeywordEnabled) {
                throw new RegoProcessorException.UncheckedBadPolicyException("The keyword *with* is not enabled");
            }
            return ruleRunner.executeWith(lctx);
        } else {
            return visit(ctx.stat_infix());
        }
    }

    private boolean hasNot(RegoParser.StatContext ctx) {
        RegoParser.LiteralContext parent = (RegoParser.LiteralContext) ctx.getParent();
        return parent.NOT() != null;
    }

    private JsonValue visitStatNOT(RegoParser.StatContext ctx, JsonValue value) {
        if (hasNot(ctx)) {
            if (value == JsonValue.FALSE) {
                return JsonValue.TRUE;
            }
            return JsonValue.FALSE;
        }
        return value;
    }

    /**
     * A Stat infix always has a query in its parent chain. A query always runs in a
     * scope setup by its parent. Query tree nodes can freely write variables to the
     * scope since these are all temporal but allow query literals to share.
     */
    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitStat_infix(RegoParser.Stat_infixContext ctx) {
        JsonValue rhs = visit(ctx.term(0));
        if (ctx.term().size() == 2) {
            OperatorType op = OperatorType.of(ctx.infix_operator().getText());
            rhs = InfixOperator.applyInfixOperator(rhs, op, visit(ctx.term(1)));
        }
        return rhs;
    }

    @Override
    public JsonValue visitRef(RegoParser.RefContext ctx) {
        JsonValue retVal = visit(ctx.getChild(0));
        if (ctx.VAR() != null) {
            retVal = ruleManager.getRuleValue(ctx.VAR().getText());
        }
        if (ctx.ref_arg() != null) {
            retVal = visitRefArgs(retVal, ctx.ref_arg());
        }
        return resolveSomeVariable(retVal);
    }

    @Override
    public JsonValue visitArray_compr(RegoParser.Array_comprContext ctx) {
        SetRule destRule = new SetRule(UUID.randomUUID().toString(), ctx.query(), true);
        return visitComprehension(ctx.query(), ctx.iterm(), null, destRule);
    }

    @Override
    public JsonValue visitSet_compr(RegoParser.Set_comprContext ctx) {
        SetRule destRule = new SetRule(UUID.randomUUID().toString(), ctx.query(), false);
        return visitComprehension(ctx.query(), ctx.iterm(), null, destRule);
    }

    @Override
    public JsonValue visitObject_compr(RegoParser.Object_comprContext ctx) {
        ObjectRule destRule = new ObjectRule(UUID.randomUUID().toString(), ctx);
        return visitComprehension(ctx.query(), ctx.object_item().getChild(0), ctx.object_item().iterm(), destRule);
    }

    private JsonValue visitComprehension(ParserRuleContext qctx, ParseTree ictx, ParseTree actx, Rule destRule) {
        IndexAssignment<ParseTree> ia = new IndexAssignment<>(ictx, actx);
        ruleRunner.executeRuleContext(Collections.emptyList(), qctx, destRule, ia, true);
        return destRule.getValue();
    }

    private JsonValue resolveSomeVariable(JsonValue value) {
        if (value instanceof SomeRule) {
            return ((SomeRule) value).getLastValue();
        }
        return value;
    }
    /**
     * An expr might point to a function defined in the policy,
     * or provided by interpreter, or registered by application.
     *
     * <p>Rego functions require their own scope.
     * Expr_call will ask {@link RuleRunner} to set up the new scope.
     * The return value is cascaded up.
     */
    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitExpr_call(RegoParser.Expr_callContext ctx) {
        List<JsonValue> args = ctx.iterm().stream().map((t) -> visit(t)).collect(Collectors.toList());
        String functionName = ctx.VAR().stream().map((v) -> v.getText()).collect(Collectors.joining("."));
        if (exprLibrary.isJavaFunction(functionName)) {
            return exprLibrary.executeJavaFunction(functionName, args);
        }
        return ruleRunner.executeRegoFunction(functionName, args);
    }

    @Override
    public JsonValue visitArray(RegoParser.ArrayContext ctx) {
        if (ctx.EMPTY_ARRAY() != null) {
            return JsonResolver.nativeToJson(Stream.empty());
        }
        return visit(ctx.non_empty_array());
    }

    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitNon_empty_array(RegoParser.Non_empty_arrayContext ctx) {
        return JsonResolver.nativeToJson(ctx.iterm().stream().map((t) -> visit(t)));
    }

    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitNon_empty_set(RegoParser.Non_empty_setContext ctx) {
        return JsonResolver.nativeToJson(ctx.iterm().stream().map((t) -> visit(t)));
    }

    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitScalar(RegoParser.ScalarContext ctx) {
        if (ctx.STRING() != null) {
            return JsonResolver.nativeToJson(ctx.STRING().getText().replaceAll("^\"|^`|`$|\"$", ""));
        } else if (ctx.NUMBER() != null) {
            return JsonResolver.nativeToJson(new BigDecimal(ctx.NUMBER().getText()));
        } else if (ctx.FALSE() != null) {
            return JsonValue.FALSE;
        } else if (ctx.TRUE() != null) {
            return JsonValue.TRUE;
        }
        return JsonValue.NULL;
    }

    @Override
    public JsonValue visitObject(RegoParser.ObjectContext ctx) {
        if (ctx.empty_object() != null) {
            return JsonResolver.nativeToJson(Collections.emptyMap());
        }
        return visit(ctx.non_empty_object());
    }

    @Override
    @SuppressWarnings("checkstyle:MethodName")
    public JsonValue visitNon_empty_object(RegoParser.Non_empty_objectContext ctx) {
        return JsonResolver.nativeToJson(
            ctx.object_item().stream().collect(Collectors.toMap(
                (i) -> JsonResolver.nativeToJson(visit(i.getChild(0))),
                (i) -> JsonResolver.nativeToJson(visit(i.iterm()))
            ))
        );
    }

    private JsonValue visitRefArgs(JsonValue state, List<RegoParser.Ref_argContext> ctx) {
        for (RegoParser.Ref_argContext ractx: ctx) {
            JsonValue key = getRefArgKey(ractx);
            state = resolveRef(ractx, state, key);
        }
        return state;
    }

    private JsonValue resolveRef(ParserRuleContext ctx, JsonValue state, JsonValue key) {
        if (key instanceof UnderscoreLocation) {
            return resolveSetRef(ctx, state, key);
        } else if (key instanceof SomeRule) {
            return resolveSomeVariable(state, key);
        }
        return JsonResolver.resolveJsonRef(state, key);
    }

    private JsonValue resolveSomeVariable(JsonValue state, JsonValue key) {
        SomeRule some = (SomeRule) key;
        JsonArray array = (JsonArray) state;
        UnderscoreLocation score = ruleManager.getUnderscoreLocation();
        JsonValue value = score.peek(some.key, array);
        some.setLastValue(score.peekIndex(some.key, array));
        return value;
    }

    private JsonValue resolveSetRef(ParserRuleContext ctx, JsonValue state, JsonValue key) {
        JsonArray set = (JsonArray) state;
        return ((UnderscoreLocation) key).peek(ctx, set);
    }

    private JsonValue getRefArgKey(RegoParser.Ref_argContext ractx) {
        if (ractx.VAR() != null) {
            return JsonResolver.nativeToJson(ractx.VAR().getText());
        } else if (ractx.ref_arg_brack().iterm() != null) {
            return visit(ractx.ref_arg_brack().iterm());
        }
        return ruleManager.getUnderscoreLocation();
    }

    @SkipMethodTrace
    public RuleManager getRuleManager() {
        return ruleManager;
    }

    @SkipMethodTrace
    public PolicyVisitor withJavaFunctions(Map<String, BaseExpression> exprMap) {
        exprLibrary.registerJavaFunctions(exprMap);
        return this;
    }

    @SkipMethodTrace
    public PolicyVisitor withKeywordEnabled(boolean withKeywordEnabled) {
        this.withKeywordEnabled = withKeywordEnabled;
        return this;
    }

    @SkipMethodTrace
    public PolicyVisitor withStrictTypeCheckEnabled(boolean strictTypeCheckEnabled) {
        exprLibrary.strictTypeCheckEnabled(strictTypeCheckEnabled);
        return this;
    }

    public void cleanupExpressions() {
        exprLibrary.cleanupExpressions();
    }
}
