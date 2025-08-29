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

import com.github.pcbouman_eur.testing.choices.ChoiceResult;
import com.github.pcbouman_eur.testing.choices.ChoicesResults;
import com.github.pcbouman_eur.testing.choices.Runner;
import com.github.pcbouman_eur.testing.choices.annotations.TestStep;
import com.github.pcbouman_eur.testing.cli.util.ClassUtils;
import picocli.CommandLine;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "run-choices", mixinStandardHelpOptions = true,
        description = "Run multiple tests sets where students can make choices, based on a specification")
public class ChoicesRunner implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description="Class name of a class containing the specification")
    private String className;

    @CommandLine.Option(names = {"-p", "--points"},
        description = "Optionally write the obtained points to a structured JSON file for Codegrade to consume")
    private Path pointsFile;


    @Override
    public Integer call() throws IOException {

        ClassUtils.initClassloader();
        Class<?> clz = ClassUtils.loadClass(className);

        PrintStream out = System.out;
        System.setOut(new PrintStream(OutputStream.nullOutputStream()));
        System.setErr(new PrintStream(OutputStream.nullOutputStream()));

        ChoicesResults results = Runner.runChoices(clz);

        if (!results.isProperRun()) {
            out.println("\u26d4 There was a problem running the tests for the different choices. \u26d4");
            out.println();
            out.println("This is likely a configuration error in the @ChoiceTests annotation of "+className);
            out.println("Contact your teacher if this problem persists");
            out.println();
            for (ChoiceResult cr : results.getProblematicChoices()) {
                if (cr.isEmpty()) {
                    out.println("\u26d4 Choice "+cr.getChoice().name()+" did not contain any TestSteps");
                }
                else {
                    out.println("\u2757 " + cr.getChoice().name() + " contained the following empty test steps");
                    for (TestStep step : cr.getEmptyRuns()) {
                        String classNames = Arrays.stream(step.testClasses())
                                .map(Class::getSimpleName)
                                .collect(Collectors.joining(", "));
                        String tags = Arrays.stream(step.tags())
                                .collect(Collectors.joining(", ", "[", "]"));
                        if (step.tags().length > 0) {
                            out.println("- Classes "+classNames+", tags="+tags);
                        }
                        else {
                            out.println("- Classes "+classNames);
                        }
                    }
                }
            }
            return 1;
        }
        else {
            out.println(results.generateReport(true));
        }

        if (pointsFile != null) {
            String json = results.getPointsJson();
            Files.writeString(pointsFile, json, StandardCharsets.UTF_8);
        }

        return 0;
    }
}
