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

/**
 * Exception that indicates a field was expected to exist but was missing
 */
public class MissingFieldException extends SymbolException {

    private final String fieldName;
    private final Class<?> expectedType;
    private final boolean staticField;
    private final Class<?> sourceClass;

    /**
     * Constructor for a MissingFieldException
     * @param fieldname the name of the field that is missing
     * @param sourceClass the class in which the field is missing
     * @param expectedType the type the missing field is supposed to have
     * @param staticField whether the field is supposed to be static
     */
    public MissingFieldException(String fieldname, Class<?> sourceClass, Class<?> expectedType, boolean staticField) {
        super(makeMsg(fieldname, sourceClass, expectedType, staticField));
        this.fieldName = fieldname;
        this.expectedType = expectedType;
        this.sourceClass = sourceClass;
        this.staticField = staticField;
    }

    /**
     * The class from which the field is missing
     * @return the class from which the field is missing
     */
    public Class<?> getSourceClass() {
        return sourceClass;
    }

    /**
     * The name of the missing field
     * @return the name of the missing field
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * The expected type of the missing field
     * @return the expected type of the missing field
     */
    public Class<?> getExpectedType() {
        return expectedType;
    }

    public boolean isStaticField() {
        return staticField;
    }

    private static String makeMsg(String fieldname, Class<?> clz, Class<?> type, boolean staticField) {
        String prefix = staticField ? "Missing static field " : "Missing field";
        return prefix + fieldname + " of type " + convertType(type) + " in class " + clz.getCanonicalName();
    }

}
