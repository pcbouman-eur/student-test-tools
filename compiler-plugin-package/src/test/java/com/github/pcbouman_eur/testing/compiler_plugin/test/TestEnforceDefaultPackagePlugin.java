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

import com.github.pcbouman_eur.testing.compiler_plugin.PackageDetectedException;
import org.junit.jupiter.api.Test;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestEnforceDefaultPackagePlugin {

    @Test
    public void testPluginException() throws IOException {
        JavaCompiler cmp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = cmp.getStandardFileManager(null, null, null)) {
            List<String> options = Collections.singletonList("-Xplugin:EnforceDefaultPackage");
            List<JavaFileObject> units = Collections.singletonList(new StringSourceCode("test.pkg.MyClass",
                    "package test.pkg;\nclass MyClass{ }"));
            JavaCompiler.CompilationTask task = cmp.getTask(null, fileManager, null, options, null, units);
            try {
                task.call();
            }
            catch (RuntimeException ex) {
                assertNotNull(ex.getCause(), "The RuntimeException thrown is expected to have a cause");
                assertEquals(PackageDetectedException.class, ex.getCause().getClass(),
                        "A PackageDetectedException should be thrown if a package is declared");
                PackageDetectedException pkgEx = (PackageDetectedException) ex.getCause();
                assertEquals("test.pkg", pkgEx.getPackageName(),
                        "Exception contains correct package name");
                assertEquals("/test/pkg/MyClass.java", pkgEx.getUnitName(),
                        "Exception contains correct compilation unit name");
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
