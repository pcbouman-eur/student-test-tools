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

package com.github.pcbouman_eur.testing.sanitze.tests;

import org.junit.jupiter.api.Test;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.platform.engine.discovery.DiscoverySelectors.*;

public class TestPlugin {

    @Test
    public void testSanitizePlugin() {
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(SanitizedTestClass.class))
                .build();
        Launcher launcher = LauncherFactory.create();
        TestPlan testPlan = launcher.discover(request);
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(testPlan);
        List<TestExecutionSummary.Failure> failures = listener.getSummary().getFailures();
        assertEquals(1, failures.size(), "The unit test should have failed");
        StackTraceElement[] trace = failures.get(0).getException().getStackTrace();
        assertEquals(3, trace.length, "The stack trace should contain 3 lines");
        assertTrue(trace[0].getClassName().contains("StudentMockClass"), "First line refers to student class");
        assertTrue(trace[1].getClassName().contains("StudentMockClass"), "Second line refers to student class");
        assertTrue(trace[2].getClassName().contains("TeacherTestClass"), "Third line refers to teacher class");
        assertTrue(trace[2].isNativeMethod(), "Third line refers to nativeMethod");
    }

}
