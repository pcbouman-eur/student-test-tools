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

package com.github.pcbouman_eur.testing.wrapper.demo;

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
