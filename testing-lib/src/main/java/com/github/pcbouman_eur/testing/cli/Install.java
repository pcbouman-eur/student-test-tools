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

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.MustacheNotFoundException;
import com.github.pcbouman_eur.testing.cli.util.MavenDownloader;
import com.github.pcbouman_eur.testing.cli.util.ShellRunner;
import org.eclipse.aether.resolution.DependencyResolutionException;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.ProtectionDomain;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * The `install` sub‑command. Sets up a convenient and easy to use environment to be used on Codegrade.
 */
@Command(name = "install",
        description = "Install the application to a system directory.")
class Install implements Callable<Integer> {

    private static final String MAIN_CLASS_NAME = Main.class.getCanonicalName();

    public static final Set<PosixFilePermission> SCRIPT_PERMISSIONS
            = PosixFilePermissions.fromString("rwxr-xr-x");

    public static final String PMD_VERSION = "7.9.0";
    public static final String PMD_ZIP = "pmd-dist-"+PMD_VERSION+"-bin.zip";
    public static final String PMD_URL = "https://github.com/pmd/pmd/releases/download/pmd_releases%2F"
            + PMD_VERSION + "/" + PMD_ZIP;

    @Option(names = {"-a", "--artifact"},
        description = "One or multiple maven artifacts to install and add to the classpath. " +
                "Example artifact specification: org.apache.commons:commons-lang3:3.12.0")
    private List<String> artifacts;

    @Option(names = {"-m", "--maven"}, defaultValue = "/opt/maven-libs",
        description = "Local maven repository used to store relevant artifacts (default: ${DEFAULT-VALUE})")
    private String mavenDir;

    /** Directory where the app will be installed. Default: /opt/sttest */
    @Option(names = {"-d", "--dir"}, defaultValue = "/opt/sttest",
            description = "Installation directory (default=${DEFAULT-VALUE})")
    private Path installDir;

    /** Name of the wrapper shell script that will be created. */
    @Option(names = {"-s", "--script-name"}, defaultValue = "sttest",
            description = "Name of the shell script to create (default=${DEFAULT-VALUE})")
    private String scriptName;

    private static final String DEFAULT_ENV_FILE = ".cg_bash_env";

    /** Optional prefix for the CLASSPATH line in ~/.cg_bash_env */
    @Option(names = {"--env-file"}, defaultValue = DEFAULT_ENV_FILE,
            description = "Path to the environment file to update, assumed related to ~. (default: ${DEFAULT-VALUE})")
    private Path envFilePath;

    @Option(names = {"-r", "--report"}, defaultValue = "report.xml",
    description = "The default file name to use for unit testing report files. (default: ${DEFAULT-VALUE})")
    private String reportFile;

    @Option(names = {"-j", "--pointsFile"}, defaultValue = "points.json",
            description = "The default file name to use for a json points file. (default: ${DEFAULT-VALUE})")
    private String pointsFile;

    @Option(names = {"-l", "--lintFile"}, defaultValue = "linter.csv",
            description = "The default file name to use for a linter file. (default: ${DEFAULT-VALUE})")
    private String lintFile;

    @Option(names = {"-cv", "--checkstyleVersion"}, defaultValue = "10.12.3",
        description = "The checkstyle version to install if not available. (default: ${DEFAULT-VALUE})")
    private String checkstyleVersion = "10.12.3";

    @Option(names = {"-cu", "--checkstyleUser"}, defaultValue = "codegrade",
            description = "The system user to install checkstyle as (default: ${DEFAULT-VALUE})")
    private String checkstyleUser;

    @Option(names = {"-cde", "--checkstyleDeductionError"}, defaultValue = "100",
        description = "Percentage deduction for checkstyle issues with severity error (default: ${DEFAULT-VALUE})")
    private int checkStyleDeductionError;
    @Option(names = {"-cdw", "--checkstyleDeductionWarning"}, defaultValue = "25",
        description = "Percentage deduction for checkstyle issues with severity warning (default: ${DEFAULT-VALUE})")
    private int checkstyleDeductionWarning;

    @Option(names = {"-cdi", "--checkstyleDeductionInfo"}, defaultValue = "5",
            description = "Percentage deduction for checkstyle issues with severity info (default: ${DEFAULT-VALUE})")
    private int checkstyleDeductionInfo;

    @Option(names = {"-pmd", "--pmdDest"}, defaultValue="/opt/pmd",
            description = "The directory where to install PMD (default: ${DEFAULT-VALUE})")
    private Path pmdDest;

    @Option(names = {"-lcp", "--localClasspath"},
        description = "Do not adjust the environment variable CLASSPATH, but include the jars in the generated scripts.")
    private boolean localClasspath;

    @Override
    public Integer call() {
        try {
            // 1) Ensure installation directory exists
            if (!Files.exists(installDir)) {
                Files.createDirectories(installDir);
                System.out.println("Created directory: " + installDir);
            } else {
                System.out.println("Directory already exists: " + installDir);
            }

            // 2) Copy the running JAR into that directory
            Path jarSource = getRunningJarPath();
            if (jarSource == null || !Files.isRegularFile(jarSource)) {
                throw new IllegalStateException(
                        "Could not determine the path of the currently executing JAR");
            }

            Path jarDest = installDir.resolve("sttest.jar");
            Files.copy(jarSource, jarDest, StandardCopyOption.REPLACE_EXISTING);
            Files.setPosixFilePermissions(jarDest, SCRIPT_PERMISSIONS);
            System.out.println("Copied JAR to: " + jarDest);

            // Find / Install Checkstyle and add
            // Disabled for now
            Optional<Path> checkStylePath = getOrInstallCheckstyle();
            checkStylePath.ifPresent(path -> System.out.println("Checkstyle found: " + path));

            // Install PMD
            boolean installPmd = installPmd();

            List<Path> artifactPaths = installArtifacts();

            ArrayList<Path> allJars = new ArrayList<>(artifactPaths);
            //checkStylePath.ifPresent(allJars::add);

            // 3) Create the wrapper shell scripts
            buildScripts(jarDest, allJars);

            // 4‑5) Update ~/.cg_bash_env
            updateEnvFile(jarDest, allJars, installDir, installPmd);

        } catch (Exception e) {
            System.err.println("[install] Error: " + e.getMessage());
            return 1;
        }
        return 0;
    }

    /* ------------------------------------------------------------------ */
    /** Returns the absolute path of the JAR that contains this class. */
    private Path getRunningJarPath() throws IOException {
        ProtectionDomain pd = Install.class.getProtectionDomain();
        if (pd == null) return null;
        java.security.CodeSource cs = pd.getCodeSource();
        if (cs == null || cs.getLocation() == null) return null;
        try {
            // URL might be "file:/path/to/jar" or a directory when running from IDE
            String path = cs.getLocation().toURI().getPath();
            Path p = Paths.get(path);
            if (!Files.isRegularFile(p)) {   // Running from classes dir, not JAR
                System.err.println("[install] Not running from a JAR (found: " + p + ")");
                return null;
            }
            return p.toAbsolutePath();
        } catch (Exception e) {
            throw new IOException("Failed to resolve jar path", e);
        }
    }

    private Optional<Path> getCheckstyle() throws IOException {
        try {
            var result = ShellRunner.run(List.of("sudo", "-u", checkstyleUser, "cg", "checkstyle", "get-jar"), 1);
            if (result.stdout.endsWith("checkstyle.jar")) {
                return Optional.of(new File(result.stdout).toPath());
            }
        } catch (InterruptedException|TimeoutException ignored) {
            // Obtaining the jar file failed
        }
        return Optional.empty();
    }

    private Optional<Path> getOrInstallCheckstyle() throws IOException {
        // TODO: this is making many assumptions on the cg command's workings
        try {
            Optional<Path> path = getCheckstyle();
            if (path.isPresent()) {
                return path;
            }
            System.out.println("Checkstyle not found. Attempting to install version "+checkstyleVersion+" as user "+checkstyleUser);
            ShellRunner.run(List.of("sudo", "-u", checkstyleUser, "cg", "checkstyle", "install", checkstyleVersion), 0);
            return getCheckstyle();
        }
        catch (InterruptedException|TimeoutException ex) {
            return Optional.empty();
        }
    }

    private boolean installPmd() throws IOException {
        try {
            // Check if PMD already seems to be installed
            if (Files.isDirectory(pmdDest)) {
                System.out.println("PMD installation directory "+pmdDest+" already exists. Skipping installation");
                return false;
            }

            // Attempt Download
            ShellRunner.Result res = ShellRunner.run(0, "wget", "--quiet", PMD_URL);
            if (res.exitCode != 0) {
                if (!res.stdout.isBlank()) {
                    System.out.println(res.stdout);
                }
                if (!res.stderr.isBlank()) {
                    System.out.println(res.stderr);
                }
                System.out.println("Error while downloading PMD. Skipping installation.");
                return false;
            }

            boolean success;
            // Unzip and install
            res = ShellRunner.run(0, "unzip", PMD_ZIP);
            if (res.exitCode != 0) {
                System.out.println("Error while unzipping PMD");
                success = false;
            }
            else {
                Path src = Path.of("pmd-bin-"+PMD_VERSION);
                Files.move(src, pmdDest);
                System.out.println("Installed PMD into directory "+pmdDest);
                success = true;
            }

            // Cleanup
            Path zip = Path.of(PMD_ZIP);
            Files.deleteIfExists(zip);

            return success;
        }
        catch (InterruptedException|TimeoutException ex) {
            System.out.println("Error while installing PMD: "+ex.getMessage());
        }
        return false;
    }

    public List<Path> installArtifacts() throws IOException {
        if (artifacts == null || artifacts.isEmpty()) {
            System.out.println("No maven artifacts are specified for installation");
            return List.of();
        }
        System.out.println("Installing "+artifacts.size()+" maven artifacts into local repository "+mavenDir);
        MavenDownloader mvn = new MavenDownloader(new File(mavenDir));
        List<Path> jars = new ArrayList<>();
        for (String artifactSpec : artifacts) {
            try {
                List<File> files = mvn.resolve(artifactSpec);
                for (File f : files) {
                    jars.add(f.toPath());
                }
                files.forEach(file -> jars.add(file.toPath()));
                System.out.println("Installed artifact "+artifactSpec);
            }
            catch (DependencyResolutionException ex) {
                System.err.println("[error] Could not resolve artifact '"+artifactSpec
                        +"'. Error details: "+ex.getMessage());
            }
        }
        return jars;
    }

    private void buildScripts(Path jar, List<Path> otherJars) throws IOException {

        List<String> classPaths = new ArrayList<>();
        classPaths.add("$CLASSPATH");
        classPaths.add("$DIR");
        if (localClasspath) {
            classPaths.add(jar.toAbsolutePath().toString());
            otherJars.forEach(p -> classPaths.add(p.toAbsolutePath().toString()));
        }
        classPaths.add(".");

        String classPath = String.join(":", classPaths);

        Map<String,Object> model = new HashMap<>();
        model.put("mainClass", MAIN_CLASS_NAME);
        model.put("classpath", classPath);
        model.put("reportFile", reportFile);
        model.put("pointsFile", pointsFile);
        model.put("lintFile", lintFile);

        MustacheFactory mf = new DefaultMustacheFactory();
        for (Class<?> clz : Main.class.getAnnotation(Command.class).subcommands()) {
            if (!clz.equals(Install.class)) {
                String command = clz.getAnnotation(Command.class).name();
                Path scriptFile = installDir.resolve(scriptName + "-" + command);
                try {
                    Mustache mustache = mf.compile("templates/" + command + ".sh.mustache");
                    StringWriter sw = new StringWriter();
                    mustache.execute(sw, model).flush();
                    String script = sw.toString()
                            .replace("\r\n", "\n")
                            .replace("\r", "\n");
                    Files.writeString(scriptFile, script, StandardCharsets.UTF_8);
                    Files.setPosixFilePermissions(scriptFile, SCRIPT_PERMISSIONS);
                    System.out.println("Created script "+scriptFile);
                }
                catch (MustacheNotFoundException ex) {
                    System.out.println("No script template found for subcommand "+command);
                }
            }
        }
    }


    /* ------------------------------------------------------------------ */
    /** Appends or updates the CLASSPATH and PATH entries in ~/.cg_bash_env. */
    private void updateEnvFile(Path jarDest, List<Path> jars, Path scriptDir, boolean installPmd) throws IOException {
        Path envFile = envFilePath != null ? envFilePath :
                Paths.get(System.getProperty("user.home")).resolve(DEFAULT_ENV_FILE);
        List<String> lines = new ArrayList<>();
        if (Files.exists(envFile)) {
            lines = Files.readAllLines(envFile);
        }

        String jarPaths = jars.stream()
                .map(path -> path.toAbsolutePath().toString())
                .collect(Collectors.joining(":"));

        List<String> checkStyleDeductions = new ArrayList<>();
        checkStyleDeductions.add("export CG_CHECKSTYLE_VERSION="+checkstyleVersion);
        checkStyleDeductions.add("export CHECKSTYLE_DEDUCTION_ERROR="+checkStyleDeductionError);
        checkStyleDeductions.add("export CHECKSTYLE_DEDUCTION_WARNING="+checkstyleDeductionWarning);
        checkStyleDeductions.add("export CHECKSTYLE_DEDUCTION_INFO="+checkstyleDeductionInfo);

        String classpathLine = "export CLASSPATH=\"$CLASSPATH:" + jarDest.toAbsolutePath() +
                ":" + jarPaths + ":.\"";

        List<String> pathEntries = new ArrayList<>();
        pathEntries.add("$PATH");
        pathEntries.add(scriptDir.toAbsolutePath().toString());
        if (installPmd) {
            pathEntries.add(pmdDest+"/bin");
        }
        String pathLine     = "export PATH=\"" + String.join(":", pathEntries) + "\"";
        // Allow for the cli tools to detect they run inside a Codegrade environment)
        //String envLine = "export "+STTEST_ENV_VARIABLE+"=CODEGRADE_ATT2";

        // Remove any existing lines that target the same directory
        List<String> newLines = lines.stream()
                .filter(l -> !l.contains("CHECKSTYLE_DEDUCTION_"))
                .filter(l -> !l.contains(jarDest.getFileName().toString()))
                .filter(l -> !l.contains(scriptDir.toAbsolutePath().toString()))
                .collect(Collectors.toList());

        newLines.addAll(checkStyleDeductions);

        //newLines.add(envLine);
        if (!localClasspath) {
            newLines.add(classpathLine);
        }
        newLines.add(pathLine);

        if (!Files.exists(envFile) || !lines.equals(newLines)) {
            Files.write(envFile, newLines, StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("Updated environment file: " + envFile);
        } else {
            System.out.println("Environment file already up‑to‑date.");
        }
    }
}
