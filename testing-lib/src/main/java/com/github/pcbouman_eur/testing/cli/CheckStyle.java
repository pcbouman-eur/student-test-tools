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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.github.pcbouman_eur.testing.cli.util.CheckStyleRunner;
import picocli.CommandLine;
import picocli.CommandLine.Option;

/**
 * Picocli sub‑command that mimics the behaviour of the official Checkstyle CLI.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * java -jar myapp.jar checkstyle --directory src/main/java --output report.json
 * }</pre>
 */
@CommandLine.Command(
        name = "checkstyle",
        description = "Run Checkstyle against a directory and write a JSON‑compatible report.",
        mixinStandardHelpOptions = true)
public class CheckStyle implements Callable<Integer> {

    @Option(names = {"-d", "--directory"},
            paramLabel = "DIR",
            description = "Directory to scan")
    private File dir;

    @Option(names = {"-o", "--output"},
            paramLabel = "FILE",
            description = "File where the report will be written. If omitted, prints to stdout.")
    private Path output;


    // Optional: allow specifying a custom Checkstyle configuration file
    @Option(names = {"--config"},
            paramLabel = "FILE",
            description = "Checkstyle XML configuration file")
    private File configFile;

    @Option(names = {"-x", "-exclude"},
            paramLabel = "FILE",
            description = "Files to exclude from checking (e.g. main files only used to run code)")
    private List<File> exclude;

    private void getFilesToProcess(File dir, List<File> target) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                getFilesToProcess(f, target);
            }
            else if (f.getName().toLowerCase().endsWith(".java")) {
                if (!exclude.contains(f)) {
                    target.add(f);
                }
            }
        }
    }

    private List<File> getFilesToProcess() {
        File scanDir = dir;
        if (scanDir == null) {
            scanDir = new File(System.getProperty("user.dir"));
        }
        List<File> list = new ArrayList<>();
        getFilesToProcess(scanDir, list);
        return list;
    }

    public String getConfigFile() {
        if (configFile != null) {
            System.out.println("Running checkstyle with teacher provided file");
            return configFile.toString();
        }
        String fixtures = System.getenv("FIXTURES");
        if (fixtures != null && !fixtures.isBlank()) {
            File fixturesFile = new File(fixtures + File.separator + "checkstyle.xml");
            if (fixturesFile.exists()) {
                System.out.println("Running checkstyle with teacher provided file");
                return fixturesFile.toString();
            }
        }
        System.out.println("Running checkstyle with default file 'sun_checks.xml'");
        return "sun_checks.xml";
    }

    @Override
    public Integer call() throws IOException {
        List<File> files = getFilesToProcess();
        return CheckStyleRunner.runCheckStyle(getConfigFile(), output, files);
    }

}
