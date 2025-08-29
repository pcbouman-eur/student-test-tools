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


package com.github.pcbouman_eur.testing.cli.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * Helper class that can run shell commands
 */

public final class ShellRunner {

    private ShellRunner() { /* utility class */ }

    /**
     * Executes a shell command.
     *
     * @param timeoutSeconds optional timeout, 0 or negative means no timeout
     * @param cmd   the command and its arguments (each element is one word)
     * @return Result containing exit code, stdout and stderr
     * @throws IOException          if I/O error occurs
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws TimeoutException     if the process spends longer than the timeout
     */

    public static Result run(long timeoutSeconds, String... cmd)
            throws IOException, InterruptedException, TimeoutException {
        return run(Arrays.asList(cmd), timeoutSeconds);
    }

    /**
     * Executes a shell command.
     *
     * @param cmd   the command and its arguments (each element is one word)
     * @param timeoutSeconds optional timeout, 0 or negative means no timeout
     * @return Result containing exit code, stdout and stderr
     * @throws IOException          if I/O error occurs
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws TimeoutException     if the process spends longer than the timeout
     */
    public static Result run(List<String> cmd, long timeoutSeconds)
            throws IOException, InterruptedException, TimeoutException {

        // 1. Log what we are about to execute
        System.out.println("Running: " + String.join(" ", cmd));

        ProcessBuilder pb = new ProcessBuilder(cmd);
        // Optional: redirect error stream into stdout if you want them together
        // pb.redirectErrorStream(true);

        Process proc = pb.start();

        // 2. Capture stdout and stderr concurrently to avoid deadlocks
        StreamGobbler outGobbler =
                new StreamGobbler(proc.getInputStream(), StandardCharsets.UTF_8);
        StreamGobbler errGobbler =
                new StreamGobbler(proc.getErrorStream(), StandardCharsets.UTF_8);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<String> outFuture = executor.submit(outGobbler);
        Future<String> errFuture = executor.submit(errGobbler);

        // 3. Wait for the process (with optional timeout)
        boolean finished;
        if (timeoutSeconds > 0) {
            finished = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                proc.destroyForcibly();
                throw new TimeoutException(
                        "Command timed out after " + timeoutSeconds + " seconds");
            }
        } else {
            proc.waitFor(); // blocks until exit
        }

        int exitCode = proc.exitValue();

        try {
            String stdout = outFuture.get(1, TimeUnit.SECONDS);
            String stderr = errFuture.get(1, TimeUnit.SECONDS);
            return new Result(String.join(" ", cmd), exitCode, stdout, stderr);
        }
        catch (ExecutionException ex) {
            throw new AssertionError("Reaching this point does not seem possible?", ex);
        }
        finally {
            executor.shutdownNow();
        }

    }

    /** Simple POJO for the result */
    public static final class Result {
        public final String cmd;
        public final int exitCode;
        public final String stdout;
        public final String stderr;

        private Result(String cmd, int ec, String out, String err) {
            this.cmd = cmd;
            this.exitCode = ec;
            this.stdout = out;
            this.stderr = err;
        }

        @Override
        public String toString() {
            return "Result{" +
                    "cmd=" + cmd +
                    ", exitCode=" + exitCode +
                    ", stdout='" + stdout.replaceAll("\\s+", " ") + '\'' +
                    ", stderr='" + stderr.replaceAll("\\s+", " ") + '\'' +
                    '}';
        }
    }

    /** Reads an InputStream into a String */
    private static final class StreamGobbler implements Callable<String> {
        private final InputStream is;
        private final java.nio.charset.Charset charset;

        StreamGobbler(InputStream is, java.nio.charset.Charset charset) {
            this.is = is;
            this.charset = charset;
        }

        @Override
        public String call() throws IOException {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, charset))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                return sb.toString().trim();
            }
        }
    }

}