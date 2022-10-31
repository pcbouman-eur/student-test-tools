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

public class PrimeGenerator {

    private int current;

    public PrimeGenerator(int i) {
        this.current = Math.max(1,i);
    }

    public int nextPrime() {
        current++;
        while (!checkPrime()) {
            current++;
        }
        return current;
    }

    public boolean checkPrime() {
        if (current == 1) {
            return false;
        }
        // Inefficient but effective
        for (int t=2; t <= Math.sqrt(current); t++) {
            if (current % t == 0) {
                return false;
            }
        }
        return true;
    }

    public void setStart(int i) {
        this.current = Math.max(1,i);
    }

}