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

package sanitize;

import com.github.pcbouman_eur.test_wrapper.WrapperFactory;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extension that Sanitizes Exceptions
 */
public class SanitizeExceptionsExtension implements TestExecutionExceptionHandler {

    private Set<String> stackTraceCutoff = new HashSet<>();

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        // Register the test class as a potential cuttoff point for the stack trace sanitizer
        processPotentialCutoffClass(extensionContext.getRequiredTestClass());
        extensionContext.getTestClass().ifPresent(this::processPotentialCutoffClass);
        throwable.setStackTrace(sanitize(throwable.getStackTrace()));
        throw throwable;
    }

    private StackTraceElement[] sanitize(StackTraceElement[] st) {
        List<StackTraceElement> result =
                Arrays.stream(st)
                      .filter(this::showTraceElement)
                      .collect(Collectors.toList());
        result.add(new StackTraceElement("TeacherTestClass", "testCaseMethod", null, -2));
        return result.toArray(new StackTraceElement[0]);
    }

    private void processPotentialCutoffClass(Class<?> clz) {
        if (clz.getAnnotation(SanitizeExceptions.class) != null) {
            stackTraceCutoff.add(clz.getName());
        }
    }

    private boolean showTraceElement(StackTraceElement st) {
        return !stackTraceCutoff.contains(st.getClassName()) &&
                !st.getClassName().startsWith("sun.reflect") &&
                !st.getClassName().startsWith("jdk.internal.reflect") &&
                !st.getClassName().endsWith("WrapperFactory");
    }



}
