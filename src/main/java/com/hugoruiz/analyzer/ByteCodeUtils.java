/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hugoruiz.analyzer;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 *
 * @author LENOVO
 */
public class ByteCodeUtils {
    private static final Map<Integer, Integer> OPCODE_TO_LOAD_MAP = Map.of(
        Opcodes.ISTORE, Opcodes.ILOAD,
        Opcodes.FSTORE, Opcodes.FLOAD,
        Opcodes.LSTORE, Opcodes.LLOAD,
        Opcodes.DSTORE, Opcodes.DLOAD,
        Opcodes.ASTORE, Opcodes.ALOAD,
        Opcodes.ALOAD, Opcodes.ALOAD
    );

    private static final Map<Integer, Integer> TYPE_TO_LOAD_MAP = Map.of(
        Type.INT, Opcodes.ILOAD,
        Type.BYTE, Opcodes.ILOAD,
        Type.SHORT, Opcodes.ILOAD,
        Type.CHAR, Opcodes.ILOAD,
        Type.BOOLEAN, Opcodes.ILOAD,
        Type.FLOAT, Opcodes.FLOAD,
        Type.LONG, Opcodes.LLOAD,
        Type.DOUBLE, Opcodes.DLOAD,
        Type.OBJECT, Opcodes.ALOAD,
        Type.ARRAY, Opcodes.ALOAD
    );

    private static final String[] CUSTOM_DESCRIPTORS = {"Z", "C"};

    private static final Map<Integer, String> OPCODE_DESCRIPTOR_MAP = Map.of(
        Opcodes.ISTORE, "(I)V",
        Opcodes.FSTORE, "(F)V",
        Opcodes.LSTORE, "(J)V",
        Opcodes.DSTORE, "(D)V",
        Opcodes.ASTORE, "(Ljava/lang/Object;)V",
        Opcodes.ALOAD, "(Ljava/lang/Object;)V"
    );

    private static final Map<Integer, String> TYPE_DESCRIPTOR_MAP = Map.of(
        Type.INT, "(I)V",
        Type.BYTE, "(I)V",
        Type.SHORT, "(I)V",
        Type.CHAR, "(I)V",
        Type.BOOLEAN, "(I)V",
        Type.FLOAT, "(F)V",
        Type.LONG, "(J)V",
        Type.DOUBLE, "(D)V",
        Type.OBJECT, "(Ljava/lang/Object;)V"
    );

    public static int getOpcodeLoad(int key, boolean isOpcode) {
        Map<Integer, Integer> loadMap = isOpcode ? OPCODE_TO_LOAD_MAP : TYPE_TO_LOAD_MAP;

        Integer opcodeLoad = loadMap.get(key);
        if (opcodeLoad == null) {
            throw new AssertionError("Invalid key: " + key);
        }

        return opcodeLoad;
    }

    public static String getPrintDescriptor(int key, String descriptor, boolean isOpcode) {
        if (Arrays.asList(CUSTOM_DESCRIPTORS).contains(descriptor)) {
            return String.format("(%s)V", descriptor);
        }

        Map<Integer, String> descriptorMap = isOpcode ? OPCODE_DESCRIPTOR_MAP : TYPE_DESCRIPTOR_MAP;

        String printDescriptor = descriptorMap.get(key);
        if (printDescriptor == null) {
            throw new AssertionError("Invalid key: " + key);
        }

        return printDescriptor;
    }

    public static boolean isOpcodeStore(int opcode) {
        return (
            opcode == Opcodes.ISTORE || opcode == Opcodes.FSTORE || opcode == Opcodes.ASTORE ||
            opcode == Opcodes.LSTORE || opcode == Opcodes.DSTORE
        );
    }

    public static boolean isMethodCallCompilerGenerated(String owner, String methodName) {
        if (methodName.equals("<init>") || methodName.equals("<clinit>")) {
            return true;
        }

        if (methodName.equals("valueOf") && !owner.equals("java/lang/String")) {
            return true;
        }

        return false;
    }

    public static boolean isCollectionChangeType(String methodOwner, String methodName) {
        if(isMapChangeMethod(methodOwner, methodName) ||
            isListChangeMethod(methodOwner, methodName) ||
            isSetChangeMethod(methodOwner, methodName) ||
            isStackChangeMethod(methodOwner, methodName) ||
            isQueueChangeMethod(methodOwner, methodName) ||
            isDequeChangeMethod(methodOwner, methodName)            
        ) {
            return true;
        }

        return false;
    }

    private static boolean isChangeMethod(String methodOwner, String methodName, Set<String> classNames, Set<String> methodNames) {
        if (classNames.contains(methodOwner) && methodNames.contains(methodName)) {
            return true;
        }
        return false;
    }

    private static boolean isMapChangeMethod(String methodOwner, String methodName) {
        Set<String> mapClasses = Set.of("java/util/Map", "java/util/HashMap", "java/util/LinkedHashMap", "java/util/TreeMap", "java/util/ConcurrentHashMap", "java/util/AbstractMap");
        Set<String> mapAlterMethods = Set.of("put", "putAll", "putIfAbsent", "remove", "clear");
        return isChangeMethod(methodOwner, methodName, mapClasses, mapAlterMethods);
    }
    
    private static boolean isListChangeMethod(String methodOwner, String methodName) {
        Set<String> listClasses = Set.of("java/util/List", "java/util/ArrayList", "java/util/LinkedList", "java/util/Vector", "java/util/AbstractList");
        Set<String> listAlterMethods = Set.of("add", "remove", "clear", "set", "sort", "addAll", "removeRange", "removeAll", "retainAll");
        return isChangeMethod(methodOwner, methodName, listClasses, listAlterMethods);
    }
    
    private static boolean isSetChangeMethod(String methodOwner, String methodName) {
        Set<String> setClasses = Set.of("java/util/Set", "java/util/HashSet", "java/util/EnumSet", "java/util/LinkedHashSet", "java/util/TreeSet", "java/util/AbstractSet");
        Set<String> setAlterMethods = Set.of("add", "remove", "clear", "addAll", "removeAll", "retainAll");
        return isChangeMethod(methodOwner, methodName, setClasses, setAlterMethods);
    }
    
    private static boolean isStackChangeMethod(String methodOwner, String methodName) {
        Set<String> stackClasses = Set.of("java/util/Stack", "java/util/Vector");
        Set<String> stackAlterMethods = Set.of("push", "pop", "add", "addAll", "remove", "removeAll", "clear", "addElement", "removeElement",
                                               "removeElementAt", "removeAllElements", "removeRange", "retainAll", "setElementAt", "insertElementAt", "set");
        return isChangeMethod(methodOwner, methodName, stackClasses, stackAlterMethods);
    }
    
    private static boolean isQueueChangeMethod(String methodOwner, String methodName) {
        Set<String> queueClasses = Set.of("java/util/Queue", "java/util/PriorityQueue", "java/util/ArrayDeque", "java/util/ConcurrentLinkedQueue", "java/util/AbstractQueue");
        Set<String> queueAlterMethods = Set.of("add", "addAll", "set", "sort", "offer", "poll", "remove", "clear");
        return isChangeMethod(methodOwner, methodName, queueClasses, queueAlterMethods);
    }
    
    private static boolean isDequeChangeMethod(String methodOwner, String methodName) {
        Set<String> dequeClasses = Set.of("java/util/Deque", "java/util/ArrayDeque", "java/util/LinkedBlockingDeque", "java/util/AbstractDeque");
        Set<String> dequeAlterMethods = Set.of("add", "addFirst", "addLast", "offer", "offerFirst", "offerLast", "removeFirst", "removeLast",
                                               "poll", "pollFirst", "pollLast", "pop", "push", "clear");
        return isChangeMethod(methodOwner, methodName, dequeClasses, dequeAlterMethods);
    } 
}
