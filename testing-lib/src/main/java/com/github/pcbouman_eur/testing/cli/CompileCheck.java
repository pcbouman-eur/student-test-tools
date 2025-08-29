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

import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Command that can check if certain classes were compiled successfully by checking if they are loaded and available
 * on the classpath.
 */

@CommandLine.Command(
        name = "compile-check",
        mixinStandardHelpOptions = true,
        description = "Check for a compile log file and verify that the supplied classes are on the classpath."
)
public class CompileCheck implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-l", "--log-file"}, defaultValue = Compile.DEFAULT_COMPILE_LOG,
            description = "Path to the compile‑log file (default: ${DEFAULT-VALUE})"
    )
    private Path logFile;

    @CommandLine.Parameters(
            paramLabel = "CLASSNAME...",
            description = "Fully‑qualified names of classes that must be present on the classpath."
    )
    private List<String> classNames = new ArrayList<>();

    /**
     * Entry point for picocli.  Returns an exit code:
     * <ul>
     *   <li>1 – compile log file exists (content printed)</li>
     *   <li>2 – one or more classes are missing</li>
     *   <li>0 – everything OK</li>
     * </ul>
     */
    @Override
    public Integer call() {
        // 1. Check for the compile‑log file
        if (Files.exists(logFile)) {
            try {
                System.out.println("=== Compile errors or warnings are present ===");
                Files.lines(logFile).forEach(System.out::println);
            } catch (IOException e) {
                System.err.println("Error reading compile log file: " + e.getMessage());
                // Still treat it as a failure but allow the missing‑class check to run
            }
            return 1;   // non‑zero exit code because the log indicates a problem
        }

        // 2. Verify that each supplied class can be loaded
        List<String> missing = new ArrayList<>();
        for (String fqcn : classNames) {
            try {
                Class.forName(fqcn, false, Thread.currentThread().getContextClassLoader());
            } catch (ClassNotFoundException e) {
                missing.add(fqcn);
            }
        }

        if (!missing.isEmpty()) {
            System.err.println("The following classes are missing but are required:");
            missing.forEach(m -> System.err.println("  " + m));
            return 2;
        }

        // 3. All checks passed
        System.out.println("All expected classes were successfully compiled.");
        return 0;
    }

}
