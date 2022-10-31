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

package com.github.pcbouman_eur.testing.soft_assert;

import com.github.pcbouman_eur.testing.sanitze.StackTraceSanitizer;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Function;

public class SoftAssertExtension implements BeforeTestExecutionCallback, TestExecutionExceptionHandler,
                                                AfterTestExecutionCallback {

    public static final String REPORT_KEY = "report";

    @Override
    public void beforeTestExecution(ExtensionContext ctx) throws Exception {
        int storageLimit = getProperty(ctx, SoftAssert::storageLimit, 1000);
        int displayLimit = getProperty(ctx, SoftAssert::displayLimit, 5);
        boolean showBooleanDetails = getProperty(ctx, SoftAssert::showBooleanDetails, false);
        boolean allowNoAssertions = getProperty(ctx, SoftAssert::allowNoAssertions, false);
        boolean immediateExceptions = getProperty(ctx, SoftAssert::immediateExceptions, true);
        int stacktraceLimit = getProperty(ctx, SoftAssert::stacktraceDisplayLimit, 1);
        boolean sanitize = getProperty(ctx, SoftAssert::sanitizeStacktrace, true);
        StackTraceSanitizer sts = sanitize ? new StackTraceSanitizer(ctx) : null;
        HardToSoft.startSoftContext(storageLimit, displayLimit, showBooleanDetails, allowNoAssertions,
                immediateExceptions, stacktraceLimit, sts);
    }

    @Override
    public void handleTestExecutionException(ExtensionContext extensionContext, Throwable throwable) throws Throwable {
        if (HardToSoft.isHardAssert()) {
            throw throwable;
        }
        HardToSoft.reportException(throwable);
        if (HardToSoft.isImmediateExceptions()) {
            throw throwable;
        }
    }

    @Override
    public void afterTestExecution(ExtensionContext ctx) throws Exception {
        SoftAssertionResult result = HardToSoft.endContext();
        if (result.getData().anyAssertions()) {
            ctx.publishReportEntry(REPORT_KEY, result.getReport());
        }
    }

    private static <E> E getProperty(ExtensionContext ctx, Function<SoftAssert,E> fun, E other) {
        return findSoftAssert(ctx.getElement().get())
                .map(fun)
                .orElse(other);
    }

    private static Optional<SoftAssert> findSoftAssert(AnnotatedElement el) {
        if (el.isAnnotationPresent(SoftAssert.class)) {
            return Optional.of(el.getAnnotation(SoftAssert.class));
        }
        if (el instanceof Method) {
            Method m = (Method) el;
            return findSoftAssert(m.getDeclaringClass());
        }
        if (el instanceof Class) {
            Class<?> clz = (Class<?>) el;
            if (clz.getSuperclass() != null) {
                return findSoftAssert(clz.getSuperclass());
            }
        }
        return Optional.empty();
    }

}
