package com.github.pcbouman_eur.testing.compiler_plugin.test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class RandomTest {

    private static final String F1 = "public class EmptyClass { }";
    private static final String F2 = "public class OtherClass {\n\tpublic static void main(String [] args) {" +
            "\n\t\tEmptyClass ec = new EmptyClass();\n\t\tec.doSomething();\n\t\tint k = ec.getSomething();\n\t}\n}";

    public static void main(String[] args) throws IOException {
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = cmp.getStandardFileManager(null, null, null)) {
            List<String> options = Collections.singletonList("-Xplugin:EnforceDefaultPackage");
            List<JavaFileObject> units = List.of(
                    new StringSourceCode("EmptyClass", F1),
                    new StringSourceCode("OtherClass", F2)
            );
            JavaCompiler.CompilationTask task = cmp.getTask(null, fileManager, null, options, null, units);
            try {
                task.call();
            } catch (RuntimeException ex) {
                ex.printStackTrace();
            }
        }
    }
}