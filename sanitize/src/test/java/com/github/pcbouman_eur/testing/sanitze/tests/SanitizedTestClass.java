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

package com.github.pcbouman_eur.testing.sanitze.tests;

import com.github.pcbouman_eur.testing.sanitze.SanitizeExceptions;
import org.junit.jupiter.api.Test;

// Since this class has the word "Test" in the middle, it will not be run by maven
// However, we can run it with JUnit in our own unit test
@SanitizeExceptions
public class SanitizedTestClass {

    // This test will fail with an unexpected exception, which should then be automatically sanitized
    @Test
    public void testWillFail() {
        StudentMockClass smc = new StudentMockClass();
        smc.doSomething();
    }


    public static class StudentMockClass {

        public void doSomething() {
            doSomethingElse();
        }

        public void doSomethingElse() {
            throw new UnsupportedOperationException("This doesn't work");
        }

    }

}
