module com.hugoruiz.javavisualizer {
    requires java.compiler;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires javafx.swing;
    requires org.objectweb.asm;
    requires org.objectweb.asm.tree;
    requires eu.mihosoft.monacofx;
    requires com.github.javaparser.core;
    requires com.github.javaparser.symbolsolver.core;
    requires com.fasterxml.jackson.databind;
    requires animated.gif.lib;

    opens com.hugoruiz.javavisualizer to javafx.fxml;
    opens com.hugoruiz.javavisualizer.modals to javafx.fxml;
    opens com.hugoruiz.models to com.fasterxml.jackson.databind;
    exports com.hugoruiz.javavisualizer;            
}
