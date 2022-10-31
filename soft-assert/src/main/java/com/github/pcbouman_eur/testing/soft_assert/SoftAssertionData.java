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

import org.opentest4j.AssertionFailedError;
import org.opentest4j.MultipleFailuresError;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

final public class SoftAssertionData {


    private final Object monitor = new Object();

    private final Map<String,Integer> successCounts = new TreeMap<>();
    private final Map<String,Integer> failureCounts = new TreeMap<>();
    private final Map<String,Integer> totalCounts = new TreeMap<>();

    private final Map<String, List<AssertionFailedError>> failures = new TreeMap<>();
    private final Set<String> truncatedFailures = Collections.synchronizedSet(new HashSet<>());
    private final Map<Class<? extends Throwable>, List<Throwable>> exceptions = new LinkedHashMap<>();

    private final int storageLimit;

    private final AtomicInteger failuresCount = new AtomicInteger();
    private final AtomicInteger exceptionCount = new AtomicInteger();
    private boolean hasSuccess;

    /**
     * Sets up a AssertionContext. Since soft assertions will keep all failed assertions in memory until the
     * tests ends, there is a storage limit. This limit is per unique message. The actual number of failures will
     * still be counted if the storage limit is exceeded.
     *
     * @param storageLimit the maximum number of failed assertions to store per unique message
     */
    public SoftAssertionData(int storageLimit) {
        this.storageLimit = storageLimit;
    }

    /**
     * Used to report a successful assertion
     * @param msg the message associated with the assertion
     */
    public void reportSuccess(String msg) {
        hasSuccess = true;
        incrementSynchronized(successCounts, msg);
        incrementSynchronized(totalCounts, msg);
    }

    /**
     * Used to report a failed assertion
     * @param afe the exception produced by the failed assertion
     * @param msg the message associated with the assertion
     */
    public void reportFailure(AssertionFailedError afe, String msg) {
        failuresCount.incrementAndGet();
        incrementSynchronized(failureCounts, msg);
        incrementSynchronized(totalCounts, msg);
        if (insertSynchronized(failures, msg, afe, storageLimit)) {
            truncatedFailures.add(msg);
        }
    }

    /**
     * Used to report an exception that occurred during test execution
     * @param t the exception that occurred
     */
    public void reportException(Throwable t) {
        exceptionCount.incrementAndGet();
        insertSynchronized(exceptions, t.getClass(), t, Integer.MAX_VALUE);
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
        for (Throwable t : mfe.getFailures()) {
            if (t instanceof MultipleFailuresError) {
                reportMultipleFailures((MultipleFailuresError) t, msg);
            }
            else if (t instanceof AssertionFailedError) {
                reportFailure((AssertionFailedError) t, msg);
            }
            else {
                reportException(t);
            }
        }
    }

    public Map<String, Integer> getTotalCounts() {
        return Collections.unmodifiableMap(totalCounts);
    }

    public boolean anyAssertions() {
        return hasSuccess || failuresCount.get() > 0 || exceptionCount.get() > 0;
    }

    public boolean hasSuccess() {
        return hasSuccess;
    }

    public Map<String, Integer> getSuccessCounts() {
        return Collections.unmodifiableMap(successCounts);
    }

    public int getFailuresCount() {
        return failuresCount.get();
    }

    public Map<String, Integer> getFailureCounts() {
        return Collections.unmodifiableMap(failureCounts);
    }

    public Map<String, List<AssertionFailedError>> getFailures() {
        Map<String, List<AssertionFailedError>> result = new TreeMap<>();
        for (Map.Entry<String, List<AssertionFailedError>> entry : failures.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return result;
    }

    public int getExceptionCount() {
        return exceptionCount.get();
    }

    public Map<Class<? extends Throwable>, List<Throwable>> getExceptions() {
        Map<Class<? extends Throwable>, List<Throwable>> result = new LinkedHashMap<>();
        for (Map.Entry<Class<? extends Throwable>, List<Throwable>> entry : exceptions.entrySet()) {
            result.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return result;
    }

    private void incrementSynchronized(Map<String,Integer> map, String key) {
        synchronized (monitor) {
            map.merge(key, 1, Integer::sum);
        }
    }

    private <K,E> boolean insertSynchronized(Map<K,List<E>> map, K key, E element, int storageLimit) {
        boolean truncated = false;
        synchronized (monitor) {
            List<E> lst = map.get(key);
            if (lst == null) {
                lst = new ArrayList<>();
                if (storageLimit > 0) {
                    lst.add(element);
                }
                map.put(key, lst);
            }
            else if (lst.size() < storageLimit) {
                lst.add(element);
            }
            else {
                truncated = true;
            }
        }
        return truncated;
    }


    public List<? extends Throwable> getAllErrors() {
        List<Throwable> errors = new ArrayList<>();
        for (List<AssertionFailedError> lst : failures.values()) {
            errors.addAll(lst);
        }
        for (List<Throwable> lst : exceptions.values()) {
            errors.addAll(lst);
        }
        return errors;
    }
}
