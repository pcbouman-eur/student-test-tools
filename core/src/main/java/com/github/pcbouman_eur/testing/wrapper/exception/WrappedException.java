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

package com.github.pcbouman_eur.testing.wrapper.exception;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.function.Supplier;

/**
 * Wrapper class used to wrap checked exceptions that can occur during the execution of student code
 * into unchecked exceptions. All methods declared in Throwable are overriden in such a way that
 * this Exception will behave like the checked exception that was wrapped.
 *
 */
public class WrappedException extends RuntimeException {

    private Throwable t;

    /**
     * Constructor based on a raw InvocationTargetException that occurred while executing student code
     * @param t the exception to wrap
     */
    public WrappedException(Throwable t) {
        super(t);
        this.t = t;
    }

    /**
     * Getter for the exception that occurred while running student code
     * @return the original exception
     */
    public Throwable getException() {
        return t;
    }

    /**
     * Processes an InvocationTargetException. If the cause was an unchecked exception, the unchecked exception is
     * thrown directly. If the cause is a checked exception, the a WrappedException is thrown.
     * @param ex the InvocationTargetException that was thrown when running student code via reflection
     * @throws RuntimeException if the cause is a RuntimeException
     * @throws Error if the cause is an Error
     * @throws WrappedException if the cause is was a checked exception, it is wrapped and thrown as a WrappedException
     */
    public static void safeThrowHelper(InvocationTargetException ex) throws RuntimeException, Error, WrappedException {
        Throwable cause = ex.getCause();
        if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        }
        if (cause instanceof  Error) {
            throw (Error) cause;
        }
        throw new WrappedException(cause);
    }

    /**
     * Tries to run a particular piece of code. If an exception occurs, it is thrown. If it is a checked exception,
     * it is thrown as a checked exception.
     * @param r a Runnable that may produce some kind of Exception
     * @throws Throwable an Exception that occurred
     */
    public static void doOrThrow(Runnable r) throws Throwable {
        try {
            r.run();
        }
        catch (WrappedException ex) {
            throw ex.t;
        }
    }

    /**
     * Tries to run a particular piece of code. If an exception occurs, it is thrown. If it is a checked exception,
     * it is thrown as a checked exception.
     * @param r a Supplier that provides some output
     * @param <E> the return type of the code to execute
     * @return the result of the supplier upon succesful completion
     * @throws Throwable an Exception that has occurred during execution
     */
    public static <E> E getOrThrow(Supplier<E> r) throws Throwable {
        try {
            return r.get();
        }
        catch (WrappedException ex) {
            throw ex.t;
        }
    }

    @Override
    public String getMessage() {
        return t.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return t.getLocalizedMessage();
    }

    @Override
    public synchronized Throwable getCause() {
        return t.getCause();
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return t.initCause(cause);
    }

    @Override
    public String toString() {
        return t.toString();
    }

    @Override
    public void printStackTrace() {
        t.printStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        t.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        t.printStackTrace(s);
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        return t.getStackTrace();
    }
}
