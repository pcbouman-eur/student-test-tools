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

package com.github.pcbouman_eur.testing.wrapper.exception;

import java.lang.reflect.Field;

/**
 * Exception used to indicate that a field has a different state than expected.
 * In this context, state refers to being either static or non-static.
 */
public class FieldStateException extends SymbolException {

    private Field field;
    private boolean shouldBeStatic;

    /**
     * Constructor for this exception
     * @param f the field for which the unexpected state was detected
     * @param shouldBeStatic whether the field was expected to be static
     */
    public FieldStateException(Field f, boolean shouldBeStatic) {
        super(makeMessage(f, shouldBeStatic));
        this.field = f;
        this.shouldBeStatic = shouldBeStatic;
    }

    /**
     * The field with the unexpected state
     * @return the field with the unexpected state
     */
    public Field getField() {
        return field;
    }


    /**
     * Whether the field was expected to be static or not
     * @return whether the field was expected to be static or not
     */
    public boolean isShouldBeStatic() {
        return shouldBeStatic;
    }

    private static String makeMessage(Field f, boolean shouldBeStatic) {
        String msgPrefix = "Field "+f.getName()+" in class "+f.getDeclaringClass().getCanonicalName();
        if (shouldBeStatic) {
            return  msgPrefix + " is not static but should be static";
        }
        return msgPrefix + " is static but should not be static";
    }
}
