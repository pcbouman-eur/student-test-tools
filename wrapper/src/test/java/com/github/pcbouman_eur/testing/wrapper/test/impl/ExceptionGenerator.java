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

package com.github.pcbouman_eur.testing.wrapper.test.impl;

public class ExceptionGenerator {

    public ExceptionGenerator() throws Exception {
        this(true);
    }

    public ExceptionGenerator(boolean shouldThrow) throws Exception {
        super();
        if (shouldThrow) {
            throw new TestCheckedException();
        }
    }

    public static ExceptionGenerator create() throws Exception {
        return new ExceptionGenerator();
    }

    public void doSomething() throws Exception {
        doSomethingStatic();
    }

    public String getSomething() throws Exception {
        return getSomethingStatic();
    }

    public static void doSomethingStatic() throws Exception {
        throw new TestCheckedException();
    }

    public static String getSomethingStatic() throws Exception {
        throw new TestCheckedException();
    }

    public static class TestCheckedException extends Exception {
        public TestCheckedException() {
            super();
        }
    }

}
