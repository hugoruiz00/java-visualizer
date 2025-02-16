/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.hugoruiz.compiler;

import com.hugoruiz.constants.Constants;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 *
 * @author LENOVO
 */
public class Compiler {
    public void saveStringCode(String code, String className) throws IOException{
        File sourceFile = new File(className);
        FileWriter writer = new FileWriter(sourceFile);
        writer.write(code);
        writer.close();
    }
    
    public int compileCode(String className) throws Exception{
        File sourceFile = new File(className);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        int compilationResult = compiler.run(null, null, new PrintStream(errorStream), "-g", sourceFile.getPath());

        if(compilationResult != 0){
            throw new Exception(errorStream.toString());
        }

        return compilationResult;
    }
    
    public List<String> executeCode(String src, String className) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java", 
            "-cp",
            "src/main/java",
            Constants.GENERATED_CODE_PACKAGE + "." + className
        );
        Process process = processBuilder.start();

        List<String> outputList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(!line.isEmpty()){
                    outputList.add(line);
                }
            }
        }

        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if(!line.isEmpty()){
                    errors.add(line);
                }
            }
        }

        int exitCode = process.waitFor();
        if(exitCode != 0){
            throw new Exception(errors.toString());
        }
        
        return outputList;
    }
}
