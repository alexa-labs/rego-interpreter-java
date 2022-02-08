// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: Apache-2.0

package com.amazon.antlr4.rego.interpreter.core.lib.debug;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.json.JsonValue;

import com.amazon.antlr4.rego.RegoBaseVisitor;
import com.amazon.antlr4.rego.RegoParser;
import com.amazon.antlr4.rego.interpreter.core.lib.debug.MethodTracer.SkipMethodTrace;
import com.amazon.antlr4.rego.interpreter.core.type.VisitDecorator;
import com.amazon.antlr4.rego.interpreter.type.Visitation;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import lombok.extern.log4j.Log4j2;

@Log4j2
@SkipMethodTrace
public class VisitCoverage extends RegoBaseVisitor<JsonValue> implements VisitDecorator {

    private Map<String, VisitationImpl> coverageMap = new TreeMap<>();

    public Map<String, Visitation> getCoverageMap() {
        return new HashMap<>(coverageMap);
    }

    @Override
    public void after(ParseTree tree) {
        updateVisitation(tree, true);
    }

    @Override
    public void error(ParseTree tree, RuntimeException e) {
        updateVisitation(tree, false);
    }

    private void updateVisitation(ParseTree node, boolean success) {
        if (node instanceof ParserRuleContext && !(node instanceof RegoParser.QueryContext)) {
            ParserRuleContext prc = (ParserRuleContext) node;
            final int line = prc.start.getLine();
            final int column = prc.start.getCharPositionInLine();
            final String key = getKey(line, column);
            VisitationImpl v = coverageMap.computeIfAbsent(key, (k) -> new VisitationImpl(line, column));
            v.setSuccess(v.getSuccess() || success);
            v.setFailure(v.getFailure() || !success);
            if (node.getParent() instanceof RegoParser.User_functionContext) {
                updateVisitation(node.getParent(), success);
            }
        }
    }

    @Override
    public void endVisit(ParseTree policyTree) {
        newVisit(policyTree);
    }

    private int lineNumber = 1;
    private int columnNumber = 0;
    private StringBuilder coverageReport;

    private void newVisit(ParseTree node) {
        lineNumber = 1;
        columnNumber = 0;
        coverageReport = new StringBuilder();
        coverageReport.append("\n");
        coverageReport.append(ANSI_CYAN);
        coverageReport.append("  1 ");
        visit(node);
        coverageReport.append(ANSI_RESET);
        log.debug(coverageReport);
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_WHITE = "\u001B[37m";

    @Override
    public JsonValue visit(ParseTree node) {
        if (node instanceof TerminalNode) {
            TerminalNode tn = (TerminalNode) node;
            int line = fixLine(tn);
            int column = fixColumn(tn);
            String colorCode = getColorCode(line, column);
            coverageReport.append(colorCode);
            coverageReport.append(node.getText());
            columnNumber += node.getText().length();
        }
        return visitChildren(node);
    }

    private JsonValue visitChildren(ParseTree node) {
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            visit(node.getChild(i));
        }
        return null;
    }

    private int fixColumn(TerminalNode tn) {
        int requiredColumn = tn.getSymbol().getCharPositionInLine();
        while (requiredColumn > columnNumber) {
            coverageReport.append(" ");
            columnNumber++;
        }
        return requiredColumn;
    }

    private int fixLine(TerminalNode tn) {
        int requiredLine = tn.getSymbol().getLine();
        while (requiredLine > lineNumber) {
            coverageReport.append(ANSI_RESET);
            coverageReport.append("\n");
            lineNumber++;
            coverageReport.append(ANSI_CYAN);
            coverageReport.append(String.format("%3d ", lineNumber));
            columnNumber = 0;
        }
        return requiredLine;
    }

    private String getColorCode(int requiredLine, int requiredColumn) {
        String key = getKey(requiredLine, requiredColumn);
        if (coverageMap.containsKey(key)) {
            return coverageToColor(coverageMap.get(key));
        }
        return ANSI_WHITE;
    }

    private String coverageToColor(VisitationImpl cov) {
        if (cov.getFailure() && cov.getSuccess()) {
            return ANSI_BLUE;
        } else if (cov.getSuccess()) {
            return ANSI_GREEN;
        }
        return ANSI_RED;
    }

    private String getKey(int line, int column) {
        return String.format("line %3d col %3d", line, column);
    }
}
