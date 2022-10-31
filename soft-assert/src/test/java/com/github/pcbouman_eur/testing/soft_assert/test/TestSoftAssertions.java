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

package com.github.pcbouman_eur.testing.soft_assert.test;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestTag;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestSoftAssertions {

    @Test
    public void testSoftAssertions() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(SoftAssertionTestClass.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(testPlan);
        TestExecutionSummary summary = listener.getSummary();
        assertEquals(4L, summary.getTestsSucceededCount(), "Correct number of tests succeeded");
        List<TestExecutionSummary.Failure> failures = summary.getFailures();
        for (TestExecutionSummary.Failure failure : failures) {
            TestIdentifier id = failure.getTestIdentifier();
            List<TestTag> tagList = new ArrayList<>(id.getTags());
            String tag = tagList.get(0).getName();
            Throwable t = failure.getException();
            checkFailure(tag, t);
        }
    }

    private void checkFailure(String testTag, Throwable t) {
        String[] tags  = testTag.split("\\s+,\\s+");
        for (String tag : tags) {
            if (Checks.FAIL.hasKey(tag)) {
                int count = Integer.parseInt(Checks.FAIL.getValue(tag));
                MultipleFailuresError mfe = (MultipleFailuresError) t;
                List<AssertionFailedError> afes = filter(mfe.getFailures(), AssertionFailedError.class);
                assertEquals(count, afes.size(), "Correct number of failures");
            } else if (Checks.EXCEPTION.hasKey(tag)) {
                String exceptionType = Checks.EXCEPTION.getValue(tag);
                if (t instanceof MultipleFailuresError) {
                    MultipleFailuresError mfe = (MultipleFailuresError) t;
                    boolean found = false;
                    for (Throwable t2 : exclude(mfe.getFailures(), AssertionFailedError.class)) {
                        boolean match = exceptionType.equals(t2.getClass().getSimpleName());
                        assertTrue(match, "Thrown Exceptions have the expected type");
                        found = true;
                    }
                    assertTrue(found, "At least one exception of the given type was found");
                }
                else {
                    assertEquals(exceptionType, t.getClass().getSimpleName(), "Thrown Exception has the expected type");
                }
            }
        }
    }

    private <T extends Throwable> List<T> filter(List<Throwable> list, Class<T> clz) {
        return list.stream()
                .filter(clz::isInstance)
                .map(clz::cast)
                .collect(Collectors.toList());
    }

    private List<Throwable> exclude(List<Throwable> list, Class<?> clz) {
        return list.stream()
                .filter(t -> !clz.isInstance(t))
                .collect(Collectors.toList());
    }

    private enum Checks {

        FAIL("fail"),
        EXCEPTION("exception");
        private final String key;

        Checks(String key) {
            this.key = key;
        }

        public boolean hasKey(String token) {
            return token.startsWith(key + "=");
        }

        public String getKey() {
            return key;
        }

        public String getValue(String token) {
            return token.substring(key.length()+1);
        }

    }

}
