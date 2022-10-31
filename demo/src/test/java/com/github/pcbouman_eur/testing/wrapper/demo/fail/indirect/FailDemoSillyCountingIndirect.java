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

package com.github.pcbouman_eur.testing.wrapper.demo.fail.indirect;

import com.github.pcbouman_eur.testing.wrapper.WrapperFactory;
import com.github.pcbouman_eur.testing.wrapper.demo.EmptyClass;
import com.github.pcbouman_eur.testing.wrapper.demo.SillyCountingInterface;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * Class that runs some tests on an implementation of the SillyCounting class
 * as described in the example assignment.
 *
 * @author Paul Bouman
 *
 */

public class FailDemoSillyCountingIndirect {

    // We define an interface SillyCountingInterface and declare a WrapperFactory here
    public static WrapperFactory<SillyCountingInterface, EmptyClass> fac =
            new WrapperFactory<>(SillyCountingInterface.class, EmptyClass.class);

    public static List<String> runCode(int start, int length)
    {
        // This constructor call was adjusted to use the WrapperFactory
        SillyCountingInterface sc = fac.constructor(start);
        List<String> answer = new ArrayList<>();
        for (int t=0; t < length; t++) {
            answer.add(sc.getNextCount());
        }
        return answer;
    }

    @Test
    public void testExample() {
        List<String> answer = runCode(6,6);
        List<String> expected = Arrays.asList("6", "NOSE", "8", "9", "10", "BARS");
        assertEquals(expected, answer, "Testing the example from the assignment document");
    }

    @Test
    public void testFrom75() {
        List<String> answer = runCode(75,4);
        List<String> expected = Arrays.asList("NOSE", "NOSE", "BARS NOSE", "NOSE");
        assertEquals(expected, answer, "Testing four numbers starting at 75, which includes a 'BARS NOSE'");
    }

    @Test
    public void testFrom85() {
        List<String> answer = runCode(85,5);
        List<String> expected = Arrays.asList("85","86","NOSE","BARS","YUMMY");
        assertEquals(expected, answer, "Testing five numbers starting at 85");
    }

    /**
     * This test checks whether some combinations of the two words are properly generated
     */
    @Test
    public void testCombinations() {
        List<String> answer, expected;

        int num = 711;
        answer = runCode(num,1);
        expected = Arrays.asList("BARS NOSE");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'BARS NOSE'");

        num = 789;
        answer = runCode(num,1);
        expected = Arrays.asList("NOSE YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'NOSE YUMMY'");

        num = 7 * 89;
        answer = runCode(num,1);
        expected = Arrays.asList("NOSE YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'NOSE YUMMY'");

        num = 1189;
        answer = runCode(num,1);
        expected = Arrays.asList("BARS YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'BARS YUMMY'");

        num = 11*4*89;
        answer = runCode(num,1);
        expected = Arrays.asList("BARS YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'BARS YUMMY'");

        num = 11 * 89;
        answer = runCode(num,1);
        expected = Arrays.asList("BARS NOSE YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'BARS NOSE YUMMY'");


        num = 71189;
        answer = runCode(num,1);
        expected = Arrays.asList("BARS NOSE YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'BARS NOSE YUMMY'");

        num = 7 * 11 * 89;
        answer = runCode(num,1);
        expected = Arrays.asList("BARS NOSE YUMMY");
        assertEquals(expected, answer, "Testing whether "+num+" gives 'BARS NOSE YUMMY'");
    }

    /**
     * This test checks whether two separate objects do not interfere with each other.
     * This can happen if the instance variable of the counter is declared in a
     * static fashion.
     */
    @Test
    public void testMultiInstance() {

        List<String> answer1 = new ArrayList<>();
        List<String> answer2 = new ArrayList<>();

        // This was a constructor call that was replaced by the WrapperFactory
        SillyCountingInterface sc1 = fac.constructor(1);
        answer1.add(sc1.getNextCount());
        // This was a constructor call that was replaced by the WrapperFactory
        SillyCountingInterface sc2 = fac.constructor(9);
        answer2.add(sc2.getNextCount());
        answer1.add(sc1.getNextCount());
        answer2.add(sc2.getNextCount());

        List<String> expected1 = Arrays.asList("1", "2");
        List<String> expected2 = Arrays.asList("9", "10");

        String msg = "Testing whether two separate SillyCounting objects do not interfere with each other.";
        msg += " If this test fails, make sure to declare your instance variables in a non-static fashion.";

        assertEquals(expected1, answer1, msg);
        assertEquals(expected2, answer2, msg);
    }


}
