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


import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class PmdCheckTest {

    @Test
    public void testBridgeRunsAndFindsViolations() throws Exception {
        // Configure Checkstyle checker
        DefaultConfiguration checkerConfig = new DefaultConfiguration("Checker");
        DefaultConfiguration treeWalkerConfig = new DefaultConfiguration("TreeWalker");

        DefaultConfiguration bridgeConfig =
                new DefaultConfiguration("pmd.PmdCheck");
        bridgeConfig.addAttribute("rule", "SystemPrintln");
        bridgeConfig.addAttribute("rule", "category/java/errorprone.xml/NullAssignment");

        treeWalkerConfig.addChild(bridgeConfig);
        checkerConfig.addChild(treeWalkerConfig);

        Checker checker = new Checker();
        checker.setModuleClassLoader(Checker.class.getClassLoader());

        // Capture audit events
        List<AuditEvent> events = new ArrayList<>();
        checker.addListener(new AuditListener() {
            @Override public void auditStarted(AuditEvent event) {}
            @Override public void auditFinished(AuditEvent event) {}
            @Override public void fileStarted(AuditEvent event) {}
            @Override public void fileFinished(AuditEvent event) {}
            @Override public void addError(AuditEvent event) { events.add(event); }
            @Override public void addException(AuditEvent event, Throwable throwable) {}
        });

        checker.configure(checkerConfig);

        // Run Checkstyle against a test file
        File testFile = new File("src/test/resources/TestFile.java");
        checker.process(java.util.Collections.singletonList(testFile));

        checker.destroy();

        // Assert that PMD violations were found
        assertFalse("Expected at least one PMD violation from the bridge", events.isEmpty());

        // Optional: print them for debugging
        for (AuditEvent event : events) {
            System.out.println(event.getMessage());
        }
    }

}
