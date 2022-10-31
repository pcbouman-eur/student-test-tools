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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "run", mixinStandardHelpOptions = true,
                description = "Run selected tests and store the results in an output XML file")
public class TestRunner implements Callable<Void> {

    private static final String CODEGRADE_OUTPUT_ENV_VARIABLE = "CG_JUNIT_XML_LOCATION";

    @CommandLine.Option(names = {"--class", "-c"}, description = "Names of classes containing testcases to run",
        required = true)
    private List<String> classNames;
    @CommandLine.Option(names = {"--tag", "-t"}, description = "Run only tests with these tags")
    private List<String> tags;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Name of file the output XML should be written to")
    private File output;

    @CommandLine.Option(names = {"-ao", "--allowOutput"}, description = "ALlows students to print to standard out")
    private boolean allowStandardOut;

    @CommandLine.Option(names = {"-ae", "--allowError"}, description = "Allows students to print to standard error")
    private boolean allowStandardError;

    private boolean suppressOutputLocation = false;

    private PrintStream out;
    private PrintStream err;
    private ByteArrayOutputStream alternativeStandardOut;
    private ByteArrayOutputStream alternativeStandardErr;

    @Override
    public Void call() throws IOException, ParserConfigurationException, TransformerException {

        if (output == null) {
            String cgOutput = System.getenv(CODEGRADE_OUTPUT_ENV_VARIABLE);
            if (cgOutput != null && !cgOutput.isBlank()) {
                output = new File(cgOutput);
                suppressOutputLocation = true;
            }
        }

        out = System.out;
        err = System.err;
        alternativeStandardOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(alternativeStandardOut, true, "utf-8"));
        alternativeStandardErr = new ByteArrayOutputStream();
        System.setOut(new PrintStream(alternativeStandardErr, true, "utf-8"));

        LauncherDiscoveryRequest request = getRequest();

        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        SummaryGeneratingListener sumListener = new SummaryGeneratingListener();
        TestDataListener listener = new TestDataListener();
        launcher.registerTestExecutionListeners(sumListener, listener);
        out.println("Running tests");
        launcher.execute(testPlan);
        out.println("Done running tests");

        PrintWriter pw = new PrintWriter(out);
        sumListener.getSummary().printTo(pw);

        if (output == null) {
            out.println("No output file defined");
            return null;
        }
        if (suppressOutputLocation) {
             out.println("Writing output XML");
        }
        else {
            out.println("Writing output XML to " + output);
        }
        JUnitLegacyXMLWriter.writeXml(output, listener);
        out.println("Done.");

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
                .map(this::loadClass)
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

    private Class<?> loadClass(String className) {
        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);
        }
        catch (ClassNotFoundException ignored) {
            // This ignored because we will make another attempt
        }
        try {
            File cwd = new File(System.getProperty("user.dir"));
            URL [] cp = {cwd.toURI().toURL()};
            URLClassLoader urlcl = new URLClassLoader(cp);
            return urlcl.loadClass(className);
        }
        catch (ClassNotFoundException cnf) {
            throw new IllegalArgumentException("Class with name "+className+" was not found.");
        }
        catch (SecurityException ex) {
            throw new AssertionError("Unexpected error. " +
                    "Unable to load classes from the current working directory.", ex);
        }
        catch (MalformedURLException ex) {
            throw new AssertionError("Unexpected error. URL generated by a File class was malformed", ex);
        }
    }

}
