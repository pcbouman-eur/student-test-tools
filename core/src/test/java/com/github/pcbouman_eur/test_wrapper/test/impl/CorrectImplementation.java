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


package com.github.pcbouman_eur.test_wrapper.test.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CorrectImplementation {

    public static String GLOBAL_MESSAGE = "This is a global test message";
    public static final int NUMBER = 12;
    public static final List<String> MY_LIST = Arrays.asList("say", "hello");


    private boolean flag = false;
    private int number = 0;
    private List<String> list = new ArrayList<>();

    public CorrectImplementation() {
        super();
    }

    public CorrectImplementation(int initialNumber, List<String> initialList) {
        super();
        number = initialNumber;
        list.addAll(initialList);
    }

    public CorrectImplementation(CorrectImplementation ci) {
        flag = ci.flag;
        number = ci.number;
        list.addAll(ci.list);
    }

    public void setFlagToTrue() {
        flag = true;
    }

    public boolean getFlag() {
        return flag;
    }

    public void setPrimitive(int k) {
        number = k;
    }

    public int getPrimitive() {
        return number;
    }

    public void setBoxed(Integer k) {
        number = k;
    }

    public Integer getBoxed() {
        return number;
    }

    public void addToList(String a, String b, String c) {
        list.add(a);
        list.add(b);
        list.add(c);
    }

    public void giveMePositive(int number) {
        if (number <= 0) {
            throw new IllegalArgumentException(number+" is not positive!");
        }
    }

    public List<String> getCurrentList() {
        return Collections.unmodifiableList(list);
    }

    public static CorrectImplementation createStatic() {
        return new CorrectImplementation(42, List.of("This", "is", "a", "better", "default"));
    }

    public static String staticMethod(String k, int repetitions) {
        return Stream.generate(() -> k)
                .limit(repetitions)
                .collect(Collectors.joining());
    }

    public static void staticAddToList(List<String> list, String token, int times) {
        for (int i=0; i < times; i++) {
            list.add(token);
        }
    }

}
