// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoParser.ItermContext;
import com.amazon.antlr4.rego.interpreter.core.lib.InfixOperator;
import com.amazon.antlr4.rego.interpreter.core.type.OperatorType;

/**
 * Resolve operators in order of precedence.
 */
public final class ItermVisitor {

    private final ItermContext ctx;
    private final PolicyVisitor policyVisitor;
    private final List<OperatorType> ops = new LinkedList<>();
    private final List<JsonValue> vals = new LinkedList<>();
    private final List<Integer> opsComputeOrder = new ArrayList<>();

    private ItermVisitor(PolicyVisitor policyVisitor, ItermContext ctx) {
        this.ctx = ctx;
        this.policyVisitor = policyVisitor;
    }

    /**
     * Visits single term directly.
     * This includes expressions inside <code>()</code>.
     * For expressions involving 2 or more terms, defers to <code>MultiItermVisitor</code>.
     */
    public static JsonValue visit(PolicyVisitor policyVisitor, ItermContext ctx) {
        if (ctx.term() != null) {
            return policyVisitor.visit(ctx.term());
        } else if (ctx.iterm().size() == 1) {
            return policyVisitor.visit(ctx.iterm(0));
        }
        return new ItermVisitor(policyVisitor, ctx).visit();
    }

    private JsonValue visit() {
        itermTreeToList();
        computeOperatorsInOrder();
        return vals.get(0);
    }

    private void computeOperatorsInOrder() {
        for (int opPrecedence: opsComputeOrder) {
            computeOperatorsOfPrecedence(opPrecedence);
        }
    }

    /**
     * Compute all operators at given (highest) precedence.
     */
    private void computeOperatorsOfPrecedence(int opPref) {
        Iterator<OperatorType> opIter = ops.iterator();
        ListIterator<JsonValue> valIter = vals.listIterator();
        JsonValue lhs = valIter.next();
        while (opIter.hasNext()) {
            OperatorType op = opIter.next();
            JsonValue rhs = valIter.next();
            if (op.getPreference() != opPref) {
                lhs = rhs;
                continue;
            }
            lhs = InfixOperator.applyInfixOperator(lhs, op, rhs);
            updateComputedOperator(opIter, valIter, lhs);
        }
    }

    /**
     * Remove operator. Replace two input values with one output value.
     */
    private void updateComputedOperator(
        Iterator<OperatorType> opIter,
        ListIterator<JsonValue> valIter, JsonValue lhs) {

        opIter.remove();
        valIter.remove();
        valIter.previous();
        valIter.remove();
        valIter.add(lhs);
    }

    /**
     * ANTLR4 will create a tree with the rightmost operator at the root.
     * Expressions inside <code>()</code> are independent trees latched to
     * one of the right leaves of the parent tree.
     */
    private void itermTreeToList() {
        Set<Integer> uniquePrecedences = new HashSet<>();
        ItermContext currCtx = ctx;
        while (currCtx.infix_operator() != null) {
            currCtx = collapseSingleItermNode(uniquePrecedences, currCtx);
        }
        vals.add(policyVisitor.visit(currCtx));

        // Tree was rooted at right. Lists need to start at left.
        Collections.reverse(ops);
        Collections.reverse(vals);

        // Order of precedences to compute.
        uniquePrecedences.stream()
            .sorted(Comparator.reverseOrder())
            .forEachOrdered(opsComputeOrder::add);
    }

    /**
     * Push operator and computed rhs into lists.
     * Update operator precedence set.
     * Return lhs.
     */
    private ItermContext collapseSingleItermNode(Set<Integer> uniquePrecedences, ItermContext currCtx) {
        String opText = currCtx.infix_operator().getText();
        OperatorType op = OperatorType.of(opText);
        ops.add(op);
        uniquePrecedences.add(op.getPreference());
        vals.add(policyVisitor.visit(currCtx.iterm(1)));
        return currCtx.iterm(0);
    }
}
