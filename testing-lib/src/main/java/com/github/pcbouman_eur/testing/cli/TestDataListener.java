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

import com.github.pcbouman_eur.testing.soft_assert.SoftAssertExtension;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TestDataListener implements TestExecutionListener {

    private List<TestIdentifier> order = Collections.synchronizedList(new ArrayList<>());
    private Map<TestIdentifier, TestData> data = new ConcurrentHashMap<>();

    private long startTime;
    private long endTime;

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        this.startTime = System.currentTimeMillis();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        this.endTime = System.currentTimeMillis();
    }

    @Override
    public void executionSkipped(TestIdentifier testIdentifier, String reason) {
        if (testIdentifier.isTest()) {
            TestData entry = new TestData(testIdentifier, reason);
            data.put(testIdentifier, entry);
            order.add(testIdentifier);
        }
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        if (testIdentifier.isTest()) {
            TestData entry = new TestData(testIdentifier);
            data.put(testIdentifier, entry);
            order.add(testIdentifier);
        }
    }

    @Override
    public void executionFinished(TestIdentifier id, TestExecutionResult result) {
        if (id.isTest()) {
            data.get(id).setResult(result);
        }
    }

    @Override
    public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
        String report = entry.getKeyValuePairs().get(SoftAssertExtension.REPORT_KEY);
        if (report != null) {
            data.get(testIdentifier).setReport(report);
        }
    }

    public List<TestData> getTestData() {
        return order.stream()
                .map(data::get)
                .collect(Collectors.toList());
    }

    public int getTotalCount() {
        return data.size();
    }

    public long getSkippedCount() {
        return data.values()
                .stream()
                .filter(TestData::isSkipped)
                .count();
    }

    public long getFailureCount() {
        return data.values()
                .stream()
                .filter(TestData::isFailure)
                .count();
    }

    public long getErrorCount() {
        return data.values()
                .stream()
                .filter(TestData::isError)
                .count();
    }

    public String getTime() {
        return formatTime(startTime, endTime);
    }


    private static String formatTime(long start, long end) {
        double diff = end - start;
        double seconds = diff / 1000;
        return String.format(Locale.ROOT, "%.3f", seconds);
    }

    public static final class TestData {
        private final TestIdentifier identifier;
        private final long started;
        private long ended;
        private String skipped;
        private TestExecutionResult result;
        private String report;

        private TestData(TestIdentifier id) {
            this.identifier = id;
            this.started = System.currentTimeMillis();
        }

        private TestData(TestIdentifier id, String reason) {
            this.identifier = id;
            this.started = System.currentTimeMillis();
            this.ended = started;
            this.skipped = reason;
        }

        private void setResult(TestExecutionResult result) {
            this.ended = System.currentTimeMillis();
            this.result = result;
        }

        private void setReport(String report) {
            this.report = report;
        }

        public String getTime() {
            return formatTime(started, ended);
        }

        public boolean isFailure() {
            if (result != null && result.getStatus() != TestExecutionResult.Status.SUCCESSFUL) {
                Optional<Throwable> opt = result.getThrowable();
                if (opt.isPresent()) {
                    Throwable t = opt.get();
                    if (t instanceof AssertionFailedError || t instanceof MultipleFailuresError) {
                        return true;
                    }
                }
            }
            return false;
        }

        public boolean isSuccess() {
            return result != null && result.getStatus() == TestExecutionResult.Status.SUCCESSFUL;
        }

        public boolean isSkipped() {
            return skipped != null;
        }

        public boolean isError() {
            return !isSkipped() && !isSuccess() && !isFailure();
        }

        public TestIdentifier getIdentifier() {
            return identifier;
        }


        public Throwable getThrowable() {
            if (result != null) {
                return result.getThrowable().orElse(null);
            }
            return null;
        }

        public String getReport() {
            return report;
        }

    }

}
