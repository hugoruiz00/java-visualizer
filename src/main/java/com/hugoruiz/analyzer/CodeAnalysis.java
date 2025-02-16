package com.hugoruiz.analyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.hugoruiz.compiler.Compiler;
import com.hugoruiz.models.CodeState;
import com.hugoruiz.models.VariableDetail;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class CodeAnalysis {
  public List<String> generateLogsForCodeExecution(String stringCode, String printKey, String sourceFile)
      throws Exception {
    LoggerInjector loggerInjector = new LoggerInjector(printKey);
    JavaparserAnalysis javaparserAnalysis = new JavaparserAnalysis(loggerInjector, printKey);
    String className = javaparserAnalysis.getClassName(stringCode);
    String fullClassName = sourceFile + className;

    Compiler compiler = new Compiler();
    compiler.saveStringCode(stringCode, fullClassName + ".java");
    compiler.compileCode(fullClassName + ".java");
    javaparserAnalysis.analyzeFile(fullClassName + ".java");

    compiler.compileCode(fullClassName + ".java");
    ClassNodeParser classNodeParser = new ClassNodeParser();
    ClassNode classNode = classNodeParser.parseClass(fullClassName + ".class");

    AsmAnalysis analyzer = new AsmAnalysis(javaparserAnalysis, loggerInjector);

    for (MethodNode methodNode : classNode.methods) {
      analyzer.analyzeMethod(methodNode);
    }

    classNodeParser.writeClass(classNode, fullClassName + ".class");
    
    List<String> logs = compiler.executeCode(sourceFile, className);

    return logs;
  }

  public List<CodeState> getLogsAsCodeStates(List<String> generatedLogs, String printKey) {
    List<CodeState> codeStates = new ArrayList<CodeState>();
    Map<String, VariableDetail> variables = new LinkedHashMap<>();
    ObjectMapper mapper = new ObjectMapper();

    for (int i = 0; i < generatedLogs.size(); i++) {
      String generatedLog = generatedLogs.get(i);
      /*
       * generatedLog format:
       * {"key":"f1750f0e-e59b-416e-b9d6-8618b5c2dba8", "lineNumber":4,
       * "type":"variableChange", "name":"str", "value":"pwwkew"}
       */
      try {
        if (generatedLog.length() == 0) {
          throw new Exception("Invalid Log");
        }

        generatedLog = generatedLog.replace("\u0000", "\\u0000");
        JsonNode jsonNode = mapper.readTree(generatedLog);
        String key = jsonNode.get("key").asText();
        if (!key.equals(printKey)) {
          throw new Exception("Invalid Log");
        }

        String type = jsonNode.get("type").asText();
        String name = jsonNode.get("name").asText();
        String value = jsonNode.get("value").asText();
        String methodName = jsonNode.get("methodName").asText();
        int lineNumber = jsonNode.get("lineNumber").asInt();

        String changeDescription = "";
        switch (type) {
          case "variableChange":
            variables.put(methodName + "-" + name, new VariableDetail(name, value, methodName));
            changeDescription = String.format("Variable %s changed to: %s in method %s", name, value, methodName);
            break;
          case "methodCall":
            String printValue = "";
            int argumentsLength = jsonNode.get("argumentsLength").asInt();
            String nextLog = (i == generatedLogs.size() - 1) ? null : generatedLogs.get(i+1);
            if ((name.equals("println") || name.equals("print")) && nextLog != null && argumentsLength > 0) {
              printValue = String.format(" with value: %s", nextLog);
            }
            changeDescription = String.format("Method %s called in method %s%s", name, methodName, printValue);
            break;
          case "return":
            changeDescription = "return statement";
            break;
        }

        Map<String, VariableDetail> currentVariables = new LinkedHashMap<>(variables);
        codeStates.add(new CodeState(methodName, lineNumber, changeDescription, currentVariables));
      } catch (Exception e) {
        // System.out.println("error on decode: "+e.getMessage());
      }
    }

    return codeStates;
  }
}
