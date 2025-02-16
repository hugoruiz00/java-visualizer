
package com.hugoruiz.analyzer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;

/**
 *
 * @author LENOVO
 */
public class LoggerInjector {
  private String printkey;

  public LoggerInjector(String printKey) {
    this.printkey = printKey;
  }
  /*static InsnList createPrintlnInsn(String message) {
        InsnList instructions = new InsnList();
        instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
        instructions.add(new LdcInsnNode(message));
        instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
        return instructions;
    }*/

  public void injectPrintVariableChange(InsnList toInject, int currentLine, int opcodeLoad, int varIndex, String varName, String printDescriptor, String methodName) {
    String log = String.format(
    "\n{\"key\":\"%s\", \"methodName\":\"%s\", \"lineNumber\":%d, \"type\":\"variableChange\", \"name\":\"%s\", \"value\":\"",
      printkey, methodName, currentLine, varName
    );
    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode(log));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false));
    
    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new VarInsnNode(opcodeLoad, varIndex));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", printDescriptor, false));

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode("\"}"));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
  }

  public void injectPrintArrayInitialization(InsnList toInject, int currentLine, int opcodeLoad, int varIndex, String varName, String varDescriptor, String methodName) {
    String log = String.format(
    "\n{\"key\":\"%s\", \"methodName\":\"%s\", \"lineNumber\":%d, \"type\":\"variableChange\", \"name\":\"%s\", \"value\":\"",
      printkey, methodName, currentLine, varName
    );
    String arrayDescriptor = varDescriptor.equals("[Ljava/lang/String;") ? "[Ljava/lang/Object;" : varDescriptor;

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode(log));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false));
    
    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new VarInsnNode(opcodeLoad, varIndex));
    toInject.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "toString", "("+arrayDescriptor+")Ljava/lang/String;", false));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false));

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode("\"}"));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
  }

  public void injectPrintMethodCall(InsnList toInject, int currentLine, MethodInsnNode methodInsn, String methodName) {
    String descriptor = methodInsn.desc;
    int argumentsLength = Type.getArgumentTypes(descriptor).length;
    String log = String.format(
    "\n{\"key\":\"%s\", \"methodName\":\"%s\", \"argumentsLength\":%d, \"lineNumber\":%d, \"type\":\"methodCall\", \"name\":\"%s\", \"value\":\"\"}",
      printkey, methodName, argumentsLength, currentLine, methodInsn.name
    );

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode(log));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));   
  }

  /**
   * @deprecated Capturing a collection change is not straightforward with ASM because the variable 
   * is not directly available when the method call is executed. Instead, use the JavaParser-based 
   * method {@link #injectPrintCollectionChange} to handle this more effectively.
   */
  @Deprecated
  public void injectAsmPrintCollectionChange(InsnList toInject, int currentLine, String variableName, int varIndex, String collectionType) {
    MethodInsnNode methodInsnNodeInject = getMethodInsnNodeInject(collectionType);

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode("{\"key\":\""+printkey+"\", \"lineNumber\":" + currentLine + ", \"type\":\"variableChange\", \"name\":\""+ variableName + "\", \"value\":\""));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false));

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new VarInsnNode(Opcodes.ALOAD, varIndex));
    toInject.add(methodInsnNodeInject);
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false));

    toInject.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"));
    toInject.add(new LdcInsnNode("\"}"));
    toInject.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false));
  }

  private MethodInsnNode getMethodInsnNodeInject(String collectionType) {
    switch (collectionType) {
      case "Map":
        return new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Map", "toString", "()Ljava/lang/String;", true);
      case "List":
        return new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/List", "toString", "()Ljava/lang/String;", true);
      case "Set":
        return new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Set", "toString", "()Ljava/lang/String;", true);
      case "Stack":
        return new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/util/Vector", "toString", "()Ljava/lang/String;", false);
      case "Queue":
        return new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Queue", "toString", "()Ljava/lang/String;", true);
      case "Deque":
        return new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Deque", "toString", "()Ljava/lang/String;", true);
    }

    return null;
  }

  public void injectPrintCollectionChange(MethodCallExpr methodCall, String methodName, String varName) {
    int lineNumber = methodCall.getBegin().get().line;
    int argumentsLength = methodCall.getArguments().size();

    String message = String.format(
      "\n{\\\"key\\\":\\\"%s\\\", \\\"methodName\\\":\\\"%s\\\", \\\"argumentsLength\\\":%d, \\\"lineNumber\\\":%d, \\\"type\\\":\\\"variableChange\\\", \\\"name\\\":\\\"%s\\\", \\\"value\\\": \\\"",
      printkey, methodName, argumentsLength, lineNumber, varName
    );

    MethodCallExpr toStringCall = new MethodCallExpr(new NameExpr(varName), "toString");
    BinaryExpr fullMessageExpr = new BinaryExpr(
      new BinaryExpr(new StringLiteralExpr(message), toStringCall, BinaryExpr.Operator.PLUS),
      new StringLiteralExpr("\\\"}"), BinaryExpr.Operator.PLUS
    );

    MethodCallExpr printCall = new MethodCallExpr("System.out.println");
    printCall.setArguments(new NodeList<>(fullMessageExpr));

    addStatement(methodCall, new ExpressionStmt(printCall), "after");
  }

  public void injectPrintArrayChange(AssignExpr assignExpr, ArrayAccessExpr arrayAccess, String methodName) {
    String arrayName = arrayAccess.getName().toString();
    int lineNumber = assignExpr.getBegin().get().line;

    String message = String.format(
      "\n{\\\"key\\\":\\\"%s\\\", \\\"methodName\\\":\\\"%s\\\", \\\"lineNumber\\\":%d, \\\"type\\\":\\\"variableChange\\\", \\\"name\\\":\\\"%s\\\", \\\"value\\\": \\\"",
      printkey, methodName, lineNumber, arrayName
    );

    BinaryExpr fullMessageExpr = new BinaryExpr(
      new BinaryExpr(new StringLiteralExpr(message), new MethodCallExpr("java.util.Arrays.toString").addArgument(arrayAccess.getName()),
        BinaryExpr.Operator.PLUS
      ),
      new StringLiteralExpr("\\\"}"), BinaryExpr.Operator.PLUS
    );

    MethodCallExpr printCall = new MethodCallExpr("System.out.println");
    printCall.setArguments(new NodeList<>(fullMessageExpr));
    addStatement(assignExpr, new ExpressionStmt(printCall), "after");
  }

  public void injectPrintReturnStatement(ReturnStmt returnStmt, String methodName) {
    int lineNumber = returnStmt.getBegin().get().line;
    String message = String.format(
      "\n{\\\"key\\\":\\\"%s\\\", \\\"methodName\\\":\\\"%s\\\", \\\"lineNumber\\\":%d, \\\"type\\\":\\\"return\\\", \\\"name\\\":\\\"\\\", \\\"value\\\":\\\"\\\"}",
      printkey, methodName, lineNumber
    );

    StringLiteralExpr fullMessageExpr = new StringLiteralExpr(message);
    fullMessageExpr = new StringLiteralExpr(message);

    MethodCallExpr printCall = new MethodCallExpr("System.out.println");
    printCall.setArguments(new NodeList<>(fullMessageExpr));
    addStatement(returnStmt, new ExpressionStmt(printCall), "before");
  }

  private void addStatement(Node targetNode, ExpressionStmt printStmt, String place) {
    targetNode.findAncestor(BlockStmt.class).ifPresent(block -> {
      NodeList<Statement> statements = block.getStatements();
      for (int i = 0; i < statements.size(); i++) {
        Node node = place.equals("after") ? targetNode.getParentNode().orElse(null) : targetNode;
        int index = place.equals("after") ? i + 1 : i;
        if (statements.get(i).equals(node)) {
          statements.add(index, printStmt);
          break;
        }
      }
    });
  }
}
