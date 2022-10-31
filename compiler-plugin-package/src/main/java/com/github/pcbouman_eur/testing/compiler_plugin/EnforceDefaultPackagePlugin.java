/* Copyright 2022 Paul Bouman

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

package com.github.pcbouman_eur.testing.compiler_plugin;

import com.sun.source.tree.*;
import com.sun.source.util.*;

public class EnforceDefaultPackagePlugin implements Plugin {

    @Override
    public String getName() {
        return "EnforceDefaultPackage";
    }

    @Override
    public void init(JavacTask task, String... args) {
        task.addTaskListener(new PluginTaskListener());
    }

    private static class PluginTaskListener implements TaskListener {

        @Override
        public void finished(TaskEvent e) {
            if (e.getKind() != TaskEvent.Kind.PARSE) {
                return;
            }
            CompilationUnitTree unit = e.getCompilationUnit();
            ExpressionTree pkgName = unit.getPackageName();
            if (pkgName != null) {
                throw new PackageDetectedException(pkgName.toString(), unit.getSourceFile().getName());
            }
        }
    }
}
