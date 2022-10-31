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

package com.github.pcbouman_eur.testing.soft_assert.test;

import com.github.pcbouman_eur.testing.soft_assert.SoftAssert;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.pcbouman_eur.testing.soft_assert.SoftAssertions.*;

@SoftAssert
public class SoftAssertionTestClass {

    @Test
    @Tag("fail=10")
    public void weirdTest() {
        for (int t=0; t < 10; t++) {
            assertEquals(t, t+1, "Numbers should be equal");
        }
    }

    @Test
    @Tag("fail=5")
    public void weirderTest() {
        for (int t=0; t < 5; t++) {
            assertEquals(t, t-1, "These numbers should also be equal");
        }
    }

    @Test
    @Tag("fail=10")
    public void strangeTest() {
        for (int t=0; t < 10; t++) {
            assertTrue(false, "This should be true, I suppose...");
        }
    }

    @Test
    @Tag("exception=RuntimeException")
    public void throwsTest() {
        throw new RuntimeException("This should not happen...");
    }

    @Test
    @Tag("exception=NoAssertionsPerformedException")
    public void forgetTests() {

    }

    @Test
    @Tag("fail=1")
    public void failAndThrowTest() {
        assertTrue(false, "this is false");
        throw new RuntimeException("This should not happen...");
    }

    @Test
    public void correctTest1() {
        assertTrue(true, "This is true!");
    }

    @Test
    public void correctTest2() {
        assertEquals(1, 1, "This is equal");
        assertEquals(1.0, 1.0, "This is equal");
        assertEquals("hi", "hi", "This is equal");
    }

    @Test
    public void correctTest3() {
        assertThrows(IllegalArgumentException.class,
                () -> {
            throw new IllegalArgumentException();
        });
    }

    @SoftAssert(allowNoAssertions = true)
    @Test
    public void correctForgetTests() {

    }

}
