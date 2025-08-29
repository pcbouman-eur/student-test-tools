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
import org.opentest4j.ValueWrapper;

import java.util.*;
import java.util.stream.Collectors;

public final class SoftAssertionTextLayout {


    private final int displayLimit;
    private final boolean displayBooleanDetails;
    private final int stacktraceDisplayLength;

    private final StackTraceSanitizer sanitizer;


    /**
     * Sets up a AssertionContext. Since soft assertions will keep all failed assertions in memory until the
     * tests ends, there is a storage limit. This limit is per unique message. The actual number of failures will
     * still be counted if the storage limit is exceeded.
     *
     * @param displayLimit the maximum number of failed assertion details to display per unique message
     * @param displayBooleanDetails whether to output details of boolean assertions in the final report
     * @param stacktraceDisplayLimit limit on number of stack trace lines, -1 means no limit
     * @param sanitizer StackTraceSanitizer to apply to stack traces before displaying theme
     */
    public SoftAssertionTextLayout(int displayLimit, boolean displayBooleanDetails, int stacktraceDisplayLimit,
                                   StackTraceSanitizer sanitizer) {
        this.displayLimit = displayLimit;
        this.displayBooleanDetails = displayBooleanDetails;
        this.stacktraceDisplayLength = stacktraceDisplayLimit;
        this.sanitizer = sanitizer;
    }

    /**
     * Produces a report of successes, failures and exceptions recorded by this AssertionContext
     *
     * @return a report of successes, failures and exception recorded from soft assertions
     */
    public String getReport(SoftAssertionData data) {
        // Compute the width for number formatting options
        int colWidth = (int) Math.floor(1 + Math.log10(
                data.getTotalCounts().values().stream().mapToInt(i -> i).max().orElse(1)));
        String format = "[ %"+colWidth+"d of %"+colWidth+"d ]";

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        if (data.hasSuccess()) {
            sb.append(reportMap(data.getSuccessCounts(), data.getTotalCounts(), null, "SUCCESS", format));
            sb.append("\n\n");
        }
        if (data.getFailuresCount() > 0) {
            sb.append(reportMap(data.getFailureCounts(), data.getTotalCounts(), data.getFailures(), "FAILURES", format));
            sb.append("\n\n");
        }
        if (data.getExceptionCount() > 0) {
            sb.append(formatLabel("EXCEPTIONS"));
            sb.append("\n\n");
            for (Map.Entry<Class<? extends Throwable>,List<Throwable>> entry : data.getExceptions().entrySet()) {
                Class<?> type = entry.getKey();
                List<Throwable> lst = entry.getValue();
                sb.append(lst.size()).append(" of type ").append(type.getSimpleName());
                for (Throwable t : lst) {
                    sb.append("\n   ")
                            .append(layoutExceptionHeader(type, t))
                            .append("\n");
                    StackTraceElement[] st = t.getStackTrace();
                    if (sanitizer != null) {
                        st = sanitizer.sanitize(st);
                    }
                    int length = stacktraceDisplayLength < 0 ? st.length : Math.min(stacktraceDisplayLength, st.length);
                    for (int i=0; i < length; i++) {
                        StackTraceElement ste = st[i];
                        sb.append("      at ")
                                .append(ste.getClassName())
                                .append(".")
                                .append(ste.getMethodName());
                        if (ste.isNativeMethod()) {
                            sb.append("(Native Method)\n");
                        }
                        else {
                            sb.append("(")
                                    .append(ste.getFileName())
                                    .append(":")
                                    .append(ste.getLineNumber())
                                    .append(")\n");
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public List<Throwable> getFormattedThrowables(List<? extends Throwable> failures) {
        List<Throwable> result = new ArrayList<>();
        List<AssertionFailedError> afes = new ArrayList<>();
        for (Throwable t : failures) {
            if (t instanceof  AssertionFailedError) {
                afes.add((AssertionFailedError) t);
            }
            else {
                result.add(t);
            }
        }
        Map<String,List<AssertionFailedError>> failureMap =
                afes.stream()
                        .collect(Collectors.groupingBy(Throwable::getMessage));
        for (Map.Entry<String,List<AssertionFailedError>> entry : failureMap.entrySet()) {
            String key = entry.getKey();
            List<AssertionFailedError> errors = entry.getValue();
            if (!errors.isEmpty()) {
                String details = getAssertionFailedErrors(entry.getValue());
                result.add(new FormatThrowable(key + "\n" + details));
            }
        }
        return result;
    }

    private StringBuilder layoutExceptionHeader(Class<?> type, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append("*")
          .append(type.getSimpleName())
          .append("*");
        if (t.getMessage() != null) {
            sb.append(" : ");
            sb.append(t.getMessage().replace("\n", " "));
        }
        return sb;
    }


    private StringBuilder reportMap(Map<String,Integer> counts, Map<String,Integer> totalCounts,
                                    Map<String,List<AssertionFailedError>> failures, String label, String format) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatLabel(label));
        sb.append("\n\n");
        for (Map.Entry<String,Integer> entry : counts.entrySet()) {
            String msg = entry.getKey();
            int successCount = entry.getValue();
            int totalCount = totalCounts.get(msg);
            sb.append(String.format(Locale.ROOT, format, successCount, totalCount));
            sb.append(" ");
            sb.append(msg);
            sb.append("\n");
            if (failures != null && displayLimit > 0) {
                List<AssertionFailedError> list = failures.get(msg);
                if (list != null) {
                    sb.append(getAssertionFailedErrors(list));
                }
            }
        }
        return sb;
    }

    private String getAssertionFailedErrors(List<AssertionFailedError> list) {
        StringBuilder sb = new StringBuilder();
        boolean truncated = false;
        int displayCount = 0;
        for (AssertionFailedError afe : list) {
            boolean actualAndExpected = afe.isActualDefined() && afe.isExpectedDefined();
            boolean shouldDisplay = displayBooleanDetails || !isBoolean(afe.getExpected());
            if (actualAndExpected && shouldDisplay) {
                if (displayCount >= displayLimit) {
                    truncated = true;
                    break;
                }
                writeAssertionFailedError(afe, sb);
                displayCount++;
            }
        }
        if (truncated) {
            sb.append("   Some failure details were omitted for reasons of brevity\n");
        }
        return sb.toString();
    }

    private void writeAssertionFailedError(AssertionFailedError afe, StringBuilder sb) {
        String expected = valueToString(afe.getExpected());
        String actual = valueToString(afe.getActual());
        sb.append("   * Expected value: '");
        sb.append(expected.replace("\n", "\\n"));
        sb.append("'\n     Actual value:   '");
        sb.append(actual.replace("\n", "\\n"));
        sb.append("'\n");
    }

    private boolean isBoolean(ValueWrapper wrapper) {
        if (wrapper == null || wrapper.getValue() == null) {
            return false;
        }
        return wrapper.getType().equals(Boolean.class);
    }

    private String valueToString(ValueWrapper wrapper) {
        if (wrapper.getValue() == null) {
            return "null";
        }
        return wrapper.getStringRepresentation();
    }

    private StringBuilder formatLabel(String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("*".repeat(label.length() + 4))
                .append("\n* ")
                .append(label)
                .append(" *\n")
                .append("*".repeat(label.length() + 4));
        return sb;
    }

    public static class FormatThrowable extends RuntimeException {

        public FormatThrowable(String message) {
            super(message);
        }

        @Override
        public String toString() {
            return getMessage();
        }
    }

}
