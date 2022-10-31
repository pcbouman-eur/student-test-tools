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

package com.github.pcbouman_eur.testing.sanitze;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.util.*;

/**
 * <p>Extension that Sanitizes Exceptions thrown during tests to get rid of irrelevant elements.
 * The stack trace of any exception that is thrown during a test is cut off at the first element that:</p>
 * <ul>
 *     <li>Has a class name that was annotated with @SanitizeExceptions</li>
 *     <li>Has a class name that starts with sun.reflect or jdk.internal.reflect</li>
 *     <li>Has a class name that ends with WrapperFactory</li>
 * </ul>
 * <p>The element where the stack trace is cut off is replaced with an "internal" element of a fake class with
 * name "TeacherTestClass" and method "testMode" without a line number or associated source file.</p>
 */
public final class SanitizeExceptionsExtension implements TestExecutionExceptionHandler {

    private Set<String> stackTraceCutoff = new HashSet<>();

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        StackTraceSanitizer sts = new StackTraceSanitizer(extensionContext);
        StackTraceElement[] sanitizedTrace = sts.sanitize(throwable.getStackTrace());
        throwable.setStackTrace(sanitizedTrace);
        throw throwable;
    }
}
