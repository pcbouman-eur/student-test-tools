package com.github.pcbouman_eur.test_wrapper.demo;

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