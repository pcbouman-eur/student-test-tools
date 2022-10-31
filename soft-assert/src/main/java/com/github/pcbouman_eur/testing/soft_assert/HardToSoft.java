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
import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

/**
 * Static class used to manage potential SoftAssertion contexts.
 *
 * The SoftAssertions are maintained using a ThreadLocal object.
 *
 * A container is initialized when a Thread calls startSoftContext, and is removed when a Thread calls endContext().
 *
 * If soft assertions are performed while no context was created, regular hard assertions will be applied instead.
 */
public class HardToSoft {

    private static final String ALREADY_STARTED_ERROR = "An assertion context has already started on this thread. " +
            "Make sure you end it before starting a new one.";

    private static ThreadLocal<AssertionContext> containers = new ThreadLocal<>();

    private static AssertionContext getContext() {
        AssertionContext ctx = containers.get();
        return ctx;
    }

    /**
     * Start a soft assertion context, which means that from this point onwards, all assertions performed by this
     * Thread on the SoftAssertions class will be recorded, but failures will be suppressed until the end of the
     * test.
     *
     * @param storageLimit the maximum limit of failed assertion details to store per assertion message
     * @param displayLimit the maximum number of failed assertion details to display when generating a report
     * @param displayBooleanDetails whether to include the details of failed boolean assertions in the report
     * @param allowNoAssertions whether it is allowed to no perform any assertions during the lifespan of the context
     * @param immediateExceptions whether Exceptions should be thrown immediately after processing
     * @param stacktraceDisplayLimit limit on the number of stack trace lines to display, -1 means no limit
     * @param sanitizer StackTraceSanitizer to apply to stack traces before displaying theme
     */
    public static void startSoftContext(int storageLimit, int displayLimit, boolean displayBooleanDetails,
                                        boolean allowNoAssertions, boolean immediateExceptions,
                                        int stacktraceDisplayLimit, StackTraceSanitizer sanitizer) {
        AssertionContext ctx = containers.get();
        if (ctx != null) {
            throw new IllegalStateException(ALREADY_STARTED_ERROR);
        }
        ctx = new AssertionContext(storageLimit, displayLimit, displayBooleanDetails, allowNoAssertions,
                immediateExceptions, stacktraceDisplayLimit, sanitizer);
        containers.set(ctx);
    }

    /**
     * Ends the current soft assertion context. The context is cleared from the ThreadLocal cache, and future
     * assertions perform by the current Thread on the SoftAssertions class will behave as hard assertions.
     *
     * If any failures or exceptions were recorded during the test execution, this method will throw a
     * MultipleFailuresError that contains detailed information related to the failures recorded.
     *
     * If no assertions were recorded, while this was not explicitly allowed in this context,
     * a NoAssertionsPerformedException is thrown.
     *
     * @throws MultipleFailuresError thrown if any failures or exceptions were recorded during the test
     * @throws NoAssertionsPerformedException if no assertions were made during the test and this was expected
     *
     * @return if no failures occurred, an object containing information on the successful assertions is returned
     */
    public static SoftAssertionResult endContext() throws MultipleFailuresError, NoAssertionsPerformedException {
        AssertionContext ctx = containers.get();
        containers.remove();
        return ctx.endContext();
    }

    public static boolean isHardAssert() {
        AssertionContext ctx = getContext();
        return ctx == null;
    }

    public static void reportSuccess(String msg) {
        AssertionContext ctx = getContext();
        ctx.reportSuccess(msg);
    }

    public static void reportMultipleFailures(MultipleFailuresError mfe, String msg) {
        AssertionContext ctx = getContext();
        ctx.reportMultipleFailures(mfe, msg);
    }

    public static void reportFailure(AssertionFailedError afe, String msg) {
        AssertionContext ctx = getContext();
        ctx.reportFailure(afe, msg);
    }

    public static void reportException(Throwable t) {
        AssertionContext ctx = getContext();
        ctx.reportException(t);
    }

    public static boolean isImmediateExceptions() {
        AssertionContext ctx = getContext();
        return ctx.isImmediateExceptions();
    }
}
