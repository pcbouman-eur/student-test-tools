/* Copyright 2025 Paul Bouman

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

package com.github.pcbouman_eur.testing.compiling;

import javax.tools.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TeacherAwareCompiler {

    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    private final TeacherCompileJob job;
    private final List<String> compilerOptions;


    private final static String DISCLAIMER =
              "******\n"
            + "* THE FOLLOWING ERRORS INDICATE YOUR CODE IS NOT ACCORDING TO THE SPECIFICATION OF THE ASSIGNMENT.\n"
            + "* PLEASE CHECK THE SPECIFICATION GIVEN IN THE ASSIGNMENT DESCRIPTION TO SOLVE THESE ERRORS.\n"
            + "******\n";

    private final EnumSet<Diagnostic.Kind> FAILURES =
            EnumSet.of(Diagnostic.Kind.ERROR, Diagnostic.Kind.MANDATORY_WARNING);

    public TeacherAwareCompiler(TeacherCompileJob job, List<String> compilerOptions) {
        this.job = job;
        this.compilerOptions = new ArrayList<>(compilerOptions);
    }

    public TeacherCompileResult compile(File classDir, boolean allowPackages) throws IOException {
        StringWriter log = new StringWriter();
        StringWriter output = new StringWriter();

        DiagnosticCollector<JavaFileObject> fmDiagnostics = new DiagnosticCollector<>();

        // Obtain compilation units for both phases
        StandardJavaFileManager stdFm =
                compiler.getStandardFileManager(fmDiagnostics, Locale.ROOT, StandardCharsets.UTF_8);
        stdFm.setLocation(StandardLocation.CLASS_OUTPUT, List.of(classDir));
        TeacherCompileJob.TeacherAwareFiles files = job.getTeacherAwareFiles(stdFm);

        // Configuration compiler options
        List<String> options = new ArrayList<>(compilerOptions);
        if (!allowPackages) {
            options.add("-Xplugin:EnforceDefaultPackage");
        }

        // 4. Create and run the task for Phase 1
        DiagnosticCollector<JavaFileObject> phase1Diagnostics = new DiagnosticCollector<>();
        log.append("Running phase 1 compilation task\n");
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, stdFm, phase1Diagnostics, options, null, files.getPhase1Files());
        boolean ok = task.call();
        log.append(ok ? "Phase 1 compilation successful\n" : "Phase 1 compilation unsuccessful\n");

        // If there are errors in the student classes, they have to fix their code first

        List<Diagnostic<? extends JavaFileObject>> student = phase1Diagnostics.getDiagnostics();
        log.append(String.valueOf(student.size())).append(" diagnostic messages collected during phase 1 compilation\n");
        boolean errors = false;
        for (Diagnostic<? extends JavaFileObject> d : student) {
            output.append(d.toString()).append("\n");
            errors = errors || FAILURES.contains(d.getKind());
        }
        if (!ok || errors) {
            return new TeacherCompileResult(false, output.toString(), log.toString(), student, files);
        }

        // If phase 1 worked out okay, now run phase 2

        DiagnosticCollector<JavaFileObject> phase2Diagnostics = new DiagnosticCollector<>();
        log.append("Running phase 2 compilation task\n");
        task = compiler.getTask(null, stdFm, phase2Diagnostics, options, null, files.getPhase2Files());
        ok = task.call();
        log.append(ok ? "Phase 2 compilation successful\n" : "Phase 2 compilation unsuccessful\n");

        // B) If there are no errors in student classes, but there are in teacher classes,
        //    their code does not adhere to the specification of the assignment.
        List<Diagnostic<? extends JavaFileObject>> teacher = phase2Diagnostics.getDiagnostics();
        log.append(String.valueOf(student.size())).append(" diagnostic messages collected during phase 2 compilation\n");
        boolean anyErrors = teacher.stream()
                .anyMatch(d -> files.isTeacherFile(d.getSource()) && FAILURES.contains(d.getKind()));
        if (anyErrors) {
            output.append(DISCLAIMER).append("\n");
            for (Diagnostic<? extends JavaFileObject> d : teacher) {
                if (files.isTeacherFile(d.getSource()) && FAILURES.contains(d.getKind())) {
                    output.append(d.toString()).append("\n");
                }
            }
            return new TeacherCompileResult(false, output.toString(), log.toString(), student, files);
        }

        return new TeacherCompileResult(ok, output.toString(), log.toString(), student, files);
    }

}
