package com.github.pcbouman_eur.testing.cli;

import com.github.pcbouman_eur.testing.choices.ChoiceResult;
import com.github.pcbouman_eur.testing.choices.ChoicesResults;
import com.github.pcbouman_eur.testing.choices.Runner;
import com.github.pcbouman_eur.testing.choices.annotations.TestStep;
import picocli.CommandLine;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(name = "run-choices", mixinStandardHelpOptions = true,
        description = "Run multiple tests sets where students can make choices, based on a specification")
public class ChoicesRunner implements Callable<Integer> {

    @CommandLine.Parameters(index = "0", description="Class name of a class containing the specification")
    private String className;

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

        return 0;
    }
}
