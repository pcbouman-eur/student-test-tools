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

package com.github.pcbouman_eur.test_wrapper.exception;

import java.lang.reflect.Method;

/**
 * Exception that indicates that a method is expected to be static.
 */
public class MethodStateException extends SymbolException {

    private Method method;

    /**
     * Constructor for a MethodStateException
     * @param m the method that is expected to be static
     */
    public MethodStateException(Method m) {
        super(makeMessage(m, m.getDeclaringClass()));
        this.method = m;
    }

    /**
     * The method that was expected to be static
     * @return the method that was expected to be static
     */
    public Method getMethod() {
        return method;
    }

    private static String makeMessage(Method m, Class<?> sourceClass) {
        return "Method "+m.getName()+" in class "+sourceClass.getCanonicalName()
                + " is not static but was called in a static way";
    }
}
