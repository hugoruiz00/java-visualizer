package com.hugoruiz.analyzer;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.resolution.types.ResolvedReferenceType;
import com.github.javaparser.resolution.types.ResolvedType;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class JavaparserAnalysis {
    private int totalLinesOfCode;
    private String printKey;
    private LoggerInjector loggerInjector;
    private final Map<Integer, Integer> logsLineNumbersMap = new HashMap<>();
    private final Map<String, Integer> parameterLineNumbersMap = new HashMap<>();

    public JavaparserAnalysis(LoggerInjector loggerInjector, String printKey) {
        this.loggerInjector = loggerInjector;
        this.printKey = printKey;

        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
    }

    public String getClassName(String code) throws FileNotFoundException {
        CompilationUnit cu = StaticJavaParser.parse(code);

        for (ClassOrInterfaceDeclaration classDecl : cu.findAll(ClassOrInterfaceDeclaration.class)) {
            Optional<MethodDeclaration> mainMethod = classDecl.getMethods().stream()
                    .filter(method -> method.getNameAsString().equals("main"))
                    .findFirst();

            if (mainMethod.isPresent()) {
                return classDecl.getNameAsString();
            }
        }

        return "DefaultClass";
    }

    public void analyzeFile(String name) throws IOException {
        File sourceFile = new File(name);
        CompilationUnit cu = StaticJavaParser.parse(sourceFile);
        LexicalPreservingPrinter.setup(cu);

        cu.findAll(MethodDeclaration.class).forEach(methodDeclaration -> {
            String methodDeclName = methodDeclaration.getNameAsString();

            for (Parameter parameter : methodDeclaration.getParameters()) {
                String paramKey = getParameterKey(methodDeclName, methodDeclaration.getParameters().size(), parameter.getNameAsString());
                int paramLineNumber = parameter.getBegin().map(pos -> pos.line).orElse(-1);
                parameterLineNumbersMap.put(paramKey, paramLineNumber);
            }

            methodDeclaration.findAll(MethodCallExpr.class).forEach(methodCall -> {
                methodCall.getScope().ifPresent(scope -> {
                    ResolvedType resolvedType = scope.calculateResolvedType();
                    ResolvedReferenceType resolvedReferenceType = resolvedType.asReferenceType();

                    String className = resolvedReferenceType.getQualifiedName();
                    String methodName = methodCall.getNameAsString();

                    if (className != null
                            && ByteCodeUtils.isCollectionChangeType(className.replace(".", "/"), methodName)) {
                        String variableName = scope.toString();
                        loggerInjector.injectPrintCollectionChange(methodCall, methodDeclName, variableName);
                    }
                });
            });

            methodDeclaration.findAll(AssignExpr.class).forEach(assignExpr -> {
                Expression target = assignExpr.getTarget();

                if (target.isArrayAccessExpr()) {
                    ArrayAccessExpr arrayAccess = target.asArrayAccessExpr();
                    loggerInjector.injectPrintArrayChange(assignExpr, arrayAccess, methodDeclName);
                }
            });

            methodDeclaration.findAll(ReturnStmt.class).forEach(returnStmt -> {
                loggerInjector.injectPrintReturnStatement(returnStmt, methodDeclName);
            });
        });

        getLineNumbersOfLogs(LexicalPreservingPrinter.print(cu));

        Files.writeString(Path.of(name), LexicalPreservingPrinter.print(cu));
    }

    private String getParameterKey(String methodName, int numOfParams, String parameterName){
        return methodName + "-" + numOfParams + "-" + parameterName;
    }

    private void getLineNumbersOfLogs(String code) {
        CompilationUnit cuRead = StaticJavaParser.parse(code);
        cuRead.findAll(MethodCallExpr.class).forEach(methodCall -> {
            methodCall.getScope().ifPresent(scope -> {
                int lineNumber = methodCall.getBegin().get().line;
                String methodName = methodCall.getNameAsString();

                if (methodName.equals("println") && methodCall.getArguments().size() > 0) {
                    if (methodCall.getArgument(0).toString().contains(printKey)) {
                        logsLineNumbersMap.put(lineNumber, 1);
                    }
                }
            });
        });

        totalLinesOfCode = cuRead.getRange().map(range -> range.getLineCount()).orElse(0);
    }

    public int getTotalLinesOfCode() {
        return totalLinesOfCode;
    }

    public Map<Integer, Integer> getLogsLineNumbersMap() {
        return logsLineNumbersMap;
    }

    public Map<String, Integer> getParameterLineNumbersMap() {
        return parameterLineNumbersMap;
    }
}
