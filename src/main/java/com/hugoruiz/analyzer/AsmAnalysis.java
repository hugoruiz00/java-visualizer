
package com.hugoruiz.analyzer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.Type;

/**
 *
 * @author LENOVO
 */
public class AsmAnalysis {
    private LoggerInjector loggerInjector;
    private Map<Integer, Integer> linesMovedMap = new HashMap<>();
    private Map<Integer, Integer> logsLineNumbersMap = new HashMap<>();
    private Map<String, Integer> parameterLineNumbersMap = new HashMap<>();

    public AsmAnalysis(JavaparserAnalysis javaparserAnalysis, LoggerInjector loggerInjector) {
        this.loggerInjector = loggerInjector;
        this.logsLineNumbersMap = javaparserAnalysis.getLogsLineNumbersMap();
        this.parameterLineNumbersMap = javaparserAnalysis.getParameterLineNumbersMap();
        this.linesMovedMap = getLinesMovedMap(javaparserAnalysis.getTotalLinesOfCode());
    }
    
    private Map<Integer, Integer> getLinesMovedMap(int totalLinesOfCode) {
        int countLinesMoved = 0;
        Map<Integer, Integer> linesMovedMap = new HashMap<>();

        for (int i = 1; i <= totalLinesOfCode; i++) {
            countLinesMoved += logsLineNumbersMap.getOrDefault(i, 0);
            linesMovedMap.put(i, countLinesMoved);
        }

        return linesMovedMap;
    }

    public void analyzeMethod(MethodNode methodNode) {
        Map<Integer, LocalVariableNode> localVariableIndexMap = getLocalVariableIndexMap(methodNode);

        InsnList instructions = methodNode.instructions;
        int currentLine = -1;
        int realCurrentLine = -1;
        for (AbstractInsnNode insn : instructions) {

            if (insn instanceof LineNumberNode) {
                LineNumberNode lineNode = (LineNumberNode) insn;
                currentLine = lineNode.line;
                realCurrentLine = currentLine - linesMovedMap.getOrDefault(currentLine, 0);
            }

            if (insn instanceof IincInsnNode) {
                IincInsnNode iincInsn = (IincInsnNode) insn;
                int var = iincInsn.var;

                if (localVariableIndexMap.containsKey(var)) {
                    LocalVariableNode localVariable = localVariableIndexMap.get(var);

                    InsnList toInject = new InsnList();
                    int opcodeLoad = ByteCodeUtils.getOpcodeLoad(Opcodes.ISTORE, true);
                    String printDescriptor = ByteCodeUtils.getPrintDescriptor(Opcodes.ISTORE, localVariable.desc, true);
                    loggerInjector.injectPrintVariableChange(toInject, realCurrentLine, opcodeLoad, var,
                            localVariable.name, printDescriptor, methodNode.name);
                    instructions.insert(insn, toInject);
                }
            }

            if (insn instanceof VarInsnNode) {
                VarInsnNode varInsnNode = (VarInsnNode) insn;
                int var = varInsnNode.var;
                int opcode = varInsnNode.getOpcode();

                if (ByteCodeUtils.isOpcodeStore(opcode) && localVariableIndexMap.containsKey(var)) {
                    LocalVariableNode localVariable = localVariableIndexMap.get(var);

                    InsnList toInject = new InsnList();
                    injectVariableChange(localVariable, toInject, opcode, realCurrentLine, var,
                            methodNode.name, true);
                    instructions.insert(insn, toInject);
                }
            }

            if (insn instanceof MethodInsnNode) {
                if (logsLineNumbersMap.containsKey(currentLine)) {
                    continue;
                }

                MethodInsnNode methodInsn = (MethodInsnNode) insn;
                InsnList toInject = new InsnList();

                boolean isCollectionType = ByteCodeUtils.isCollectionChangeType(methodInsn.owner, methodInsn.name);
                if (isCollectionType) {
                    continue;
                }

                if (ByteCodeUtils.isMethodCallCompilerGenerated(methodInsn.owner, methodInsn.name)) {
                    continue;
                }

                loggerInjector.injectPrintMethodCall(toInject, realCurrentLine, methodInsn, methodNode.name);
                instructions.insertBefore(insn, toInject);
            }
        }

        injectMethodArguments(methodNode, localVariableIndexMap);
    }

    private void injectMethodArguments(MethodNode methodNode, Map<Integer, LocalVariableNode> localVariableIndexMap) {
        Type[] argumentTypes = Type.getArgumentTypes(methodNode.desc);
        boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;

        int stackIndex = isStatic ? 0 : 1;
        InsnList toInject = new InsnList();
        for (int i = 0; i < argumentTypes.length; i++) {
            Type argType = argumentTypes[i];

            if (localVariableIndexMap.containsKey(stackIndex)) {
                LocalVariableNode localVariable = localVariableIndexMap.get(stackIndex);
                String paramKey = getParameterKey(methodNode.name, argumentTypes.length, localVariable.name);
                int lineNumber = parameterLineNumbersMap.getOrDefault(paramKey, -1);

                injectVariableChange(localVariable, toInject, argType.getSort(), lineNumber, stackIndex,
                        methodNode.name, false);
            }

            stackIndex += argType.getSize();
        }
        methodNode.instructions.insert(toInject);
    }

    private void injectVariableChange(LocalVariableNode localVariable, InsnList toInject, int opcodeKey,
            int lineNumber, int varIndex, String methodName, boolean isNormalVar) {
        if (localVariable.desc.contains("[")) {
            int opcodeLoad = ByteCodeUtils.getOpcodeLoad(opcodeKey, isNormalVar);
            loggerInjector.injectPrintArrayInitialization(toInject, lineNumber, opcodeLoad, varIndex,
                    localVariable.name, localVariable.desc, methodName);
        } else {
            int opcodeLoad = ByteCodeUtils.getOpcodeLoad(opcodeKey, isNormalVar);
            String printDescriptor = ByteCodeUtils.getPrintDescriptor(opcodeKey, localVariable.desc, isNormalVar);
            loggerInjector.injectPrintVariableChange(toInject, lineNumber, opcodeLoad, varIndex,
                    localVariable.name, printDescriptor, methodName);
        }
    }

    private String getParameterKey(String methodName, int numOfParams, String parameterName) {
        return methodName + "-" + numOfParams + "-" + parameterName;
    }

    private Map<Integer, LocalVariableNode> getLocalVariableIndexMap(MethodNode methodNode) {
        Map<Integer, LocalVariableNode> localVariableIndexMap = new HashMap<>();
        List<LocalVariableNode> localVariables = methodNode.localVariables;
        for (LocalVariableNode localVariable : localVariables) {
            localVariableIndexMap.put(localVariable.index, localVariable);
        }

        return localVariableIndexMap;
    }
}
