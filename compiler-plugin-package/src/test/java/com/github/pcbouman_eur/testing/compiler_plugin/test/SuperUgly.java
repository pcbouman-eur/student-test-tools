package com.github.pcbouman_eur.testing.compiler_plugin.test;

public class SuperUgly {

    public static void main(String [] args) {
        System.out.println(superUgly());

        int j = 0;
        for (int i=0; i < 100; i++) {
            j = j++;
        }

    }

    public static String superUgly() {
        for (int t=0; t < 10; t++) {
            try {
                return "one thing";
            }
            finally {
                continue;
            }
        }
        return "another thing";
    }

}
