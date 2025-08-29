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

package com.github.pcbouman_eur.testing.cli;

import com.github.pcbouman_eur.testing.compiling.TeacherAwareCompiler;
import com.github.pcbouman_eur.testing.compiling.TeacherAwareDiagnostic;
import com.github.pcbouman_eur.testing.compiling.TeacherCompileJob;
import com.github.pcbouman_eur.testing.compiling.TeacherCompileResult;
import org.checkerframework.checker.units.qual.A;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command that can be used to compile student and teacher code.
 *
 * In general there are three types of code:
 *
 * 1) Provided source code. These are files the teacher provides to students and which students should not change.
 * 2) Student source code. These are files the student uploads as their solution to an assignment.
 * 3) Teacher source code. This is typically source code used by the teacher to test the student code.
 *
 * The compilation occurs in two phases:
 *
 * 1) The provided source code and student source code is compiled together. If any compilation errors occur,
 *    these are presented to the student as there mistake, since it occurs in code they can also work with on
 *    their own device. Only if this step occurs without errors, the second phase is used.
 * 2) The provided source code, student source code and teacher source code are compiled together. Any compilation
 *    errors now must occur in the teacher source code, which would typically indicate the student made a mistake
 *    in implementing the specification of the assignment correctly. Hence, the output of the compiler is enhanced
 *    with a disclaimer that the student should carefully check if their code adheres to the specification.
 */

@CommandLine.Command(name = "compile", mixinStandardHelpOptions = true,
        description = "Compile student and teacher code with improved feedback and relevant features")

public class Compile implements Callable<Integer> {

    public static final String DEFAULT_COMPILE_LOG = ".compile-log.txt";

    @CommandLine.Unmatched
    List<String> compilerOptions;

    @CommandLine.Option(names={"-ap", "-allowPackages"}, description = "Allow package definitions in the code")
    private boolean allowPackages;

    @CommandLine.Option(names={"-se", "--suppressErr"}, description = "Suppress compiler output to standard error")
    private boolean suppressStdErr;

    @CommandLine.Option(names={"-t", "--teacherSrc"}, description="Defines one or more sources for the teacher. " +
        "Compilation errors in teacher sources are treated differently as this suggests the student not " +
        "correctly implementing the specification in the assignment. Warnings in teacher code are suppressed.")
    private List<Path> teacherSrc;

    @CommandLine.Option(names={"-s", "--studentSrc"}, description="Defines one or more sources for the student. " +
        "Compilation errors in student code will also be generated as linter output for the student to see exactly " +
        "where the problems are in their source code."
    )
    private List<Path> studentSrc;

    @CommandLine.Option(names={"-p", "--providedSrc"}, description="Defines one or more sources that were provided" +
            "to the student and which are necessary dependencies to compile the student code."
    )
    private List<Path> providedSrc;

    @CommandLine.Option(names = {"-c", "-classDir"}, defaultValue=".",
            description="The directory to write class file to (default value: ${DEFAULT-VALUE})")
    private File classDir;

    @CommandLine.Option(names={"-o", "--output"}, defaultValue = DEFAULT_COMPILE_LOG,
        description = "File to write the result of the compilation step to")
    private File compileOutput;

    @CommandLine.Option(names={"-l", "--linter"}, description="Write linter output to standard out that can be " +
            "processed by an auto-grading environment")
    private File linterOutput;

    @CommandLine.Option(names={"-v", "--verbose"}, description="Verbose compiler output")
    private boolean verbose;

    @Override
    public Integer call() throws Exception {
        List<Path> provided = new ArrayList<>();
        List<Path> student = new ArrayList<>();
        List<Path> teacher = new ArrayList<>();
        if (providedSrc != null) {
            provided.addAll(providedSrc);
        }
        if (studentSrc != null && !studentSrc.isEmpty()) {
            student.addAll(studentSrc);
        }
        else {
            // Use the local directory instead
            student.add(Path.of("."));
        }
        if (teacherSrc != null && !teacherSrc.isEmpty()) {
            teacher.addAll(teacherSrc);
        }
        else {
            // See if FIXTURES is defined as an environment variables and add that if it is.
            String fixtures = System.getenv("FIXTURES");
            if (fixtures != null) {
                teacher.add(Path.of(fixtures));
            }
        }

        List<String> options = compilerOptions != null ? compilerOptions : List.of();
        TeacherCompileJob job = TeacherCompileJob.forSources(provided, student, teacher);
        if (verbose) {
            System.out.println(job.getSummary());
        }
        TeacherAwareCompiler tac = new TeacherAwareCompiler(job, options);
        TeacherCompileResult result = tac.compile(classDir, allowPackages);
        if (verbose) {
            System.out.println(result.getLog());
        }

        String err = result.getOutput();
        if (!suppressStdErr && !err.isBlank()) {
                System.err.print(err);
        }

        if (linterOutput != null) {
            try (PrintWriter pw = new PrintWriter(linterOutput)) {
                for (TeacherAwareDiagnostic d : result.getDiagnostics()) {
                    if (!d.isTeacherCode()) {
                        pw.println(d.toGenericLinterFeedback());
                    }
                }
            }
        }

        if (compileOutput != null && !err.isBlank()) {
            try {
                Files.writeString(compileOutput.toPath(), err, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }
            catch (IOException ex) {
                System.err.println("Error while writing compilation output: "+ex.getMessage());
            }
        }

        if (result.isOk()) {
            return 0;
        }
        return 1;
    }
}
