package com.github.pcbouman_eur.test_wrapper.demo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SillyCounting {

    private int current;

    public SillyCounting(int i) {
        this.current = i;
    }

    public String getNextCount() {
        String asString = ""+current;
        List<String> tokens = new ArrayList<>();
        if (asString.contains("11") || current % 11 == 0) {
            tokens.add("BARS");
        }
        if (asString.contains("7") || current % 7 == 0) {
            tokens.add("NOSE");
        }
        if (asString.contains("89") || current % 89 == 0) {
            tokens.add("YUMMY");
        }
        if (tokens.isEmpty()) {
            tokens.add(asString);
        }
        current++;
        return tokens.stream().collect(Collectors.joining(" "));
    }

}
