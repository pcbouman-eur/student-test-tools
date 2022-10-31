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
 * Class that provides a contextual container that can be used to capture the successes and failures of unit tests
 * that uses soft assertions, i.e. assertions that to do not terminate the tests. This is useful for functional
 * testing using basic assertions.
 */

final class AssertionContext {

    private final SoftAssertionData data;
    private final SoftAssertionTextLayout layout;

    private final boolean allowNoAssertions;
    private final boolean immediateExceptions;

    /**
     * Sets up a AssertionContext. Since soft assertions will keep all failed assertions in memory until the
     * tests ends, there is a storage limit. This limit is per unique message. The actual number of failures will
     * still be counted if the storage limit is exceeded.
     *
     * @param storageLimit the maximum number of failed assertions to store per unique message
     * @param displayLimit the maximum number of failed assertion details to display per unique message
     * @param displayBooleanDetails whether to output details of boolean assertions in the final report
     * @param allowNoAssertions whether it is allowed to have a test perform no assertions at all
     * @param immediateExceptions whether Exceptions should be thrown directly after processing
     * @param stacktraceDisplayLimit limit on number of stack trace lines, -1 means no limit
     * @param sanitizer StackTraceSanitizer to apply to stack traces before displaying theme
     */
    public AssertionContext(int storageLimit, int displayLimit, boolean displayBooleanDetails,
                            boolean allowNoAssertions, boolean immediateExceptions, int stacktraceDisplayLimit,
                            StackTraceSanitizer sanitizer) {
        this.data = new SoftAssertionData(storageLimit);
        this.layout = new SoftAssertionTextLayout(displayLimit, displayBooleanDetails, stacktraceDisplayLimit, sanitizer);
        this.allowNoAssertions = allowNoAssertions;
        this.immediateExceptions = immediateExceptions;
    }

    /**
     * Used to report a successful assertion
     * @param msg the message associated with the assertion
     */
    public void reportSuccess(String msg) {
        data.reportSuccess(msg);
    }

    /**
     * Used to report a failed assertion
     * @param afe the exception produced by the failed assertion
     * @param msg the message associated with the assertion
     */
    public void reportFailure(AssertionFailedError afe, String msg) {
        data.reportFailure(afe, msg);
    }

    /**
     * Used to report an exception that occurred during test execution
     * @param t the exception that occurred
     */
    public void reportException(Throwable t) {
        data.reportException(t);
    }

    /**
     * Used to report multiple failures that occurred during test execution.
     * This method is particularly needed to handle the assertAll() types of functions
     * that perform multiple assertions using a single assertion message.
     *
     * @param mfe the MultipleFailuresError that contains multiple failures
     * @param msg the message associated with the assertion.
     */
    public void reportMultipleFailures(MultipleFailuresError mfe, String msg) {
        data.reportMultipleFailures(mfe, msg);
    }

    /**
     * <p>This ends the soft assertion context. If any failures or exceptions occurred, a MultipleFailuresError
     * is produced (even in case of single error), and the message of this Error is an extensive report with
     * the succeeded and failed tests.
     * </p><p>
     * In case it is not allowed to perform no assertions, this method may also produce a NoAssertionsProvidedException
     * which indicates that a test was run using soft assertions, but that no assertions were performed.
     * </p>
     *
     * @throws MultipleFailuresError if any failures or exceptions were recorded by this AssertionContext
     * @throws NoAssertionsPerformedException if no assertions were performs while this was required
     *
     * @return if no failures occurred, an object containing information on the successful assertions is returned
     */
    public SoftAssertionResult endContext() throws MultipleFailuresError, NoAssertionsPerformedException {
        if (!allowNoAssertions && !data.anyAssertions()) {
            throw new NoAssertionsPerformedException();
        }
        if (data.getFailuresCount() + data.getExceptionCount() == 0) {
            return new SoftAssertionResult(data, layout);
        }
        throw new SoftAssertionFailuresError(data, layout);
    }


    /**
     * Returns whether exceptions should be thrown directly after processing
     * @return whether exceptions should be thrown directly after processing
     */
    public boolean isImmediateExceptions() {
        return immediateExceptions;
    }

}
