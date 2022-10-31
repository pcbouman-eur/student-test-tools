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

import java.util.List;

/**
 * Exception that indicates a static method is missing
 */
public class MissingStaticMethodException extends SymbolException {

    private final String methodName;
    private final Class<?> sourceType;
    private final Class<?> returnType;
    private final List<Class<?>> argTypes;

    /**
     * Constructor for a MissingStaticMethodException
     * @param sourceType the class in which the static method is missing
     * @param methodName the name of the missing static method
     * @param returnType the return type of the missing static method
     * @param argTypes a list with the argument types that make of the signature of the missing static method
     */
    public MissingStaticMethodException(Class<?> sourceType, String methodName, Class<?> returnType, List<Class<?>> argTypes) {
        super(makeMessage(sourceType, methodName, returnType, argTypes));
        this.methodName = methodName;
        this.sourceType = sourceType;
        this.returnType = returnType;
        this.argTypes = argTypes;
    }

    /**
     * The class in which the static method is missing
     * @return the class in which the static method is missing
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * The name of the missing static method
     * @return the name of the missing static method
     */
    public Class<?> getSourceType() {
        return sourceType;
    }

    /**
     * The return type of the missing static method
     * @return the return type of the missing static method
     */
    public Class<?> getReturnType() {
        return returnType;
    }

    /**
     * A list with the argument types that make of the signature of the missing static method
     * @return a list with the argument types that make of the signature of the missing static method
     */
    public List<Class<?>> getArgTypes() {
        return argTypes;
    }

    private static String makeMessage(Class<?> sourceType, String methodName, Class<?> returnType,
                                      List<Class<?>> argTypes) {
        return "Missing static method "+methodName+" with return type "+convertType(returnType)
                +" and argument types "+convertTypes(argTypes)+" in type "+convertType(sourceType);
    }

}
