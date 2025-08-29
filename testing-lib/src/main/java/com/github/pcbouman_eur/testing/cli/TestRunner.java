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

package com.github.pcbouman_eur.testing.cli;

import com.github.pcbouman_eur.testing.cli.util.AutogradeV2JsonWriter;
import com.github.pcbouman_eur.testing.cli.util.ClassUtils;
import com.github.pcbouman_eur.testing.cli.util.JUnitLegacyXMLWriter;
import com.github.pcbouman_eur.testing.cli.util.TestDataListener;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import picocli.CommandLine;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", mixinStandardHelpOptions = true,
                description = "Run selected tests and store the results in an output XML file")
public class TestRunner implements Callable<Void> {

    @CommandLine.Option(names = {"--class", "-c"}, description = "Names of classes containing testcases to run",
        required = true)
    private List<String> classNames;
    @CommandLine.Option(names = {"--tag", "-t"}, description = "Run only tests with these tags")
    private List<String> tags;

    @CommandLine.Option(names = {"-d", "--dependency"}, description = "Names of classes that are dependecies which " +
            "should be loaded before running the test")
    private List<String> dependencies;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Name of file the output should be written to")
    private File output;

    @CommandLine.Option(names = {"-ao", "--allowOutput"}, description = "ALlows students to print to standard out")
    private boolean allowStandardOut;

    @CommandLine.Option(names = {"-ae", "--allowError"}, description = "Allows students to print to standard error")
    private boolean allowStandardError;

    @CommandLine.Option(names = {"-j", "--json"}, description = "Writes a AutotestV2 json file rather than XML")
    private boolean useJson;

    @CommandLine.Option(names = {"-s", "--silent"}, description = "Suppress printing process information to stdout")
    private boolean silent;

    @CommandLine.Option(names = {"-so", "--suppressOutput"},
            description = "Suppress the output file location when printing process information " +
                    "(only relevant when non-silent)")
    private boolean suppressOutputLocation;

    private void println(String str, PrintStream out) {
        if (!silent) {
            out.println(str);
        }
    }

    @Override
    public Void call() throws IOException, ParserConfigurationException, TransformerException {

        PrintStream out = System.out;
        PrintStream err = System.err;
        ByteArrayOutputStream alternativeStandardOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(alternativeStandardOut, true, StandardCharsets.UTF_8));
        ByteArrayOutputStream alternativeStandardErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(alternativeStandardErr, true, StandardCharsets.UTF_8));

        ClassUtils.initClassloader();

        if (dependencies != null) {
            for (String dependency : dependencies) {
                ClassUtils.loadClass(dependency);
            }
        }

        LauncherDiscoveryRequest request = getRequest();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        SummaryGeneratingListener sumListener = new SummaryGeneratingListener();
        TestDataListener listener = new TestDataListener();
        launcher.registerTestExecutionListeners(sumListener, listener);
        println("Running tests", out);
        launcher.execute(testPlan);
        println("Done running tests", out);

        if (!silent) {
            PrintWriter pw = new PrintWriter(out);
            sumListener.getSummary().printTo(pw);
        }

        if (output == null) {
            err.println("No output file defined");
            return null;
        }
        if (suppressOutputLocation) {
             println("Writing output file", out);
        }
        else {
            println("Writing output file to " + output, out);
        }

        if  (!useJson) {
            JUnitLegacyXMLWriter.writeXml(output, listener);
        }
        else {
            AutogradeV2JsonWriter.write(output, listener);
        }

        println("Done.", out);

        if (allowStandardOut) {
            out.println();
            out.println("---- Standard output from student ----");
            out.println(alternativeStandardOut.toString(StandardCharsets.UTF_8));
        }

        if (allowStandardError) {
            err.println("---- Standard error from student ----");
            err.println(alternativeStandardErr.toString(StandardCharsets.UTF_8));
        }

        return null;
    }

    private LauncherDiscoveryRequest getRequest() {
        ClassSelector[] classSelectors = classNames.stream()
                .map(ClassUtils::loadClass)
                .map(DiscoverySelectors::selectClass)
                .toArray(ClassSelector[]::new);

        if (tags != null && !tags.isEmpty()) {
            return LauncherDiscoveryRequestBuilder.request()
                    .selectors(classSelectors)
                    .filters(TagFilter.includeTags(tags))
                    .build();
        }
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(classSelectors)
                .build();
    }

}
