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
import java.lang.reflect.Modifier;

/**
 * Indicates a field has a different type than what is expected
 */
public class FieldTypeException extends SymbolException {

    private final Field field;
    private final Class<?> expectedType;

    /**
     * Constructor for a FieldTypeException
     * @param field the field with the unexpected type
     * @param expectedType the type that was expected
     */
    public FieldTypeException(Field field, Class<?> expectedType) {
        super(makeMsg(field, field.getDeclaringClass(), expectedType, Modifier.isStatic(field.getModifiers())));
        this.field = field;
        this.expectedType = expectedType;
    }

    /**
     * The field that has an unexpected type
     * @return the the field that has an unexpected type
     */
    public Field getField() {
        return field;
    }

    /**
     * The type that the field was expected to have
     * @return the type that the field was expected to have
     */
    public Class<?> getExpectedType() {
        return expectedType;
    }

    private static String makeMsg(Field field, Class<?> sourceClass, Class<?> expectedType,
                                  boolean staticField) {
        String prefix;

        if (staticField) {
            prefix = "Static field ";
        }
        else {
            prefix = "Field ";
        }

        return prefix + field.getName() + " in class "+sourceClass.getCanonicalName() + " has type "
                + convertType(field.getType()) + " while type "+convertType(expectedType) + " was expected";
    }
}
