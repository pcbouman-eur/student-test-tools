package com.github.pcbouman_eur.test_wrapper.demo.fail.direct;

import com.github.pcbouman_eur.test_wrapper.demo.EmptyClass;
import com.github.pcbouman_eur.test_wrapper.demo.PrimeGenerator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class does not compile because the class that will be tested is not complete
 *
 * Class that intends to run some tests on an implementation of the PrimeGenerator class
 * as described in the example assignment.
 *
 * @author Paul Bouman
 *
 */

public class TestPrimeGeneratorDirect {

    private static List<Integer> runCode(int start, int length)
    {
        EmptyClass gen = new EmptyClass(start);
        List<Integer> answer = new ArrayList<>();
        for (int t=0; t < length; t++) {
            answer.add(gen.nextPrime());
        }
        return answer;
    }

    @Test
    public void testFirstTen() {
        List<Integer> answer = runCode(0,10);
        List<Integer> expected = Arrays.asList(2, 3, 5, 7, 11, 13, 17, 19, 23, 29);
        assertEquals(expected, answer, "Testing whether the first ten prime numbers are correctly generated");
    }

    @Test
    public void testNegativeStart() {
        List<Integer> answer = runCode(-10,1);
        List<Integer> expected = Collections.singletonList(2);
        assertEquals(expected, answer, "Testing whether two is given as the first prime when the start value is negative");

    }

    @Test
    public void testSkipCurrent() {
        List<Integer> expected = Collections.singletonList(3);
        List<Integer> answer = runCode(2,1);
        assertEquals(expected, answer, "Testing whether the starting value is not returned if it turns out to be prime");
    }

    @Test
    public void testSetStart() {
        PrimeGenerator gen = new PrimeGenerator(0);

        Integer answer, expected;

        answer = gen.nextPrime();
        expected = 2;
        assertEquals(expected, answer, "Testing whether the setStart() method works as expected");

        gen.setStart(7);
        answer = gen.nextPrime();
        expected = 11;
        assertEquals(expected, answer, "Testing whether the setStart() method works as expected");

        gen.setStart(17);
        answer = gen.nextPrime();
        expected = 19;
        assertEquals(expected, answer, "Testing whether the setStart() method works as expected");

        gen.setStart(22);
        answer = gen.nextPrime();
        expected = 23;
        assertEquals(expected, answer, "Testing whether the setStart() method works as expected");

        gen.setStart(0);
        answer = gen.nextPrime();
        expected = 2;
        assertEquals(expected, answer, "Testing whether the setStart() method works as expected");
    }

    @Test
    public void testMultiInstance() {

        String msg = "Testing whether two separate PrimeGenerator objects do not interfere with each other.";
        msg += " If this test fails, make sure to declare your instance variables in a non-static fashion.";


        PrimeGenerator gen1 = new PrimeGenerator(0);
        Integer ans1 = gen1.nextPrime();
        Integer exp1 = 2;
        assertEquals(exp1, ans1, msg);

        PrimeGenerator gen2 = new PrimeGenerator(10);
        Integer ans2 = gen2.nextPrime();
        Integer exp2 = 11;
        assertEquals(exp2, ans2, msg);

        Integer ans3 = gen1.nextPrime();
        Integer exp3 = 3;
        assertEquals(exp3, ans3, msg);

        Integer ans4 = gen2.nextPrime();
        Integer exp4 = 13;
        assertEquals(exp4, ans4, msg);

        gen1.setStart(21);
        Integer ans5 = gen2.nextPrime();
        Integer exp5 = 17;
        assertEquals(exp5, ans5, msg);

        Integer ans6 = gen1.nextPrime();
        Integer exp6 = 23;
        assertEquals(exp6, ans6, msg);
    }

}