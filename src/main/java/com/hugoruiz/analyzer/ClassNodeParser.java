/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hugoruiz.analyzer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

/**
 *
 * @author LENOVO
 */
public class ClassNodeParser {
    public ClassNode parseClass(String className) throws IOException {
        FileInputStream fis = new FileInputStream(className);
        ClassReader classReader = new ClassReader(fis);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        fis.close();
        return classNode;
    }

    public void writeClass(ClassNode classNode, String className) throws IOException {
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        classNode.accept(classWriter);
        byte[] bytecode = classWriter.toByteArray();

        try (FileOutputStream fos = new FileOutputStream(className)) {
            fos.write(bytecode);
        }
    }
}

