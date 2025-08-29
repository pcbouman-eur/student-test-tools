package com.github.pcbouman_eur.testing.compiler_plugin.test;/* Copyright 2022 Paul Bouman

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

import org.junit.jupiter.api.Test;

import javax.tools.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestEnforceDefaultPackagePlugin {

    @Test
    public void testPluginException() throws IOException {
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = cmp.getStandardFileManager(null, null, null)) {
            List<String> options = Collections.singletonList("-Xplugin:EnforceDefaultPackage");
            List<JavaFileObject> units = Collections.singletonList(new StringSourceCode("test.pkg.MyClass",
                    "package test.pkg;\nclass MyClass{ }"));
            DiagnosticCollector<JavaFileObject> diag = new DiagnosticCollector<>();
            JavaCompiler.CompilationTask task = cmp.getTask(null, fileManager, diag, options, null, units);
            try {
                task.call();
                assertEquals(1,diag.getDiagnostics().size(), "One compilation error should be produced");
                Diagnostic<? extends JavaFileObject> error = diag.getDiagnostics().get(0);
                String msg = error.getMessage(Locale.ENGLISH);
                assertEquals(Diagnostic.Kind.ERROR, error.getKind(), "The compilation error should have type 'Error'");
                assertEquals("/test/pkg/MyClass.java", error.getSource().getName(), "The source should be MyClass.java");
                assertTrue(msg.contains("test.pkg"), "The package 'test.pkg' should occur in the error");
            }
            catch (RuntimeException ex) {
                fail("The compilation should not result in an exception");
            }
        }
    }

    @Test
    public void testPluginCompilation() throws IOException {
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = cmp.getStandardFileManager(null, null, null)) {
            List<String> options = Collections.singletonList("-Xplugin:EnforceDefaultPackage");
            List<JavaFileObject> units = Collections.singletonList(new StringSourceCode("MyClass",
                    "class MyClass{ }"));
            JavaCompiler.CompilationTask task = cmp.getTask(null, fileManager, null, options, null, units);
            task.call();

        }
    }

    @Test
    public void testRegularCompilation() throws IOException {
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = cmp.getStandardFileManager(null, null, null)) {
            List<String> options = Collections.emptyList();
            List<JavaFileObject> units = Collections.singletonList(new StringSourceCode("test.pkg.MyClass",
                    "package test.pkg;\nclass MyClass{ }"));
            JavaCompiler.CompilationTask task = cmp.getTask(null, fileManager, null, options, null, units);
            task.call();
        }
    }



}
