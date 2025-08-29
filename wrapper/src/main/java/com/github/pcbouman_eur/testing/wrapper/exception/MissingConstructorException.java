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
 * Exception that is used to indicate that a constructor that was expected to exist does not
 */
public class MissingConstructorException extends SymbolException {

    private final List<Class<?>> argumentTypes;
    private final Class<?> sourceType;

    /**
     * Constructor for a MissingConstructorException
     * @param argumentTypes the argument types that make up the signature of the exception that was missing
     * @param sourceType the class in which the constructor was missing
     */
    public MissingConstructorException(List<Class<?>> argumentTypes, Class<?> sourceType) {
        super("Missing constructor in class "+convertType(sourceType)+" with arguments "+convertTypes(argumentTypes));
        this.argumentTypes = argumentTypes;
        this.sourceType = sourceType;
    }

    /**
     * A list with the argument types that make up the signature of the constructor that was expected to be missing
     * @return a list with the argument types that make up the signature of the missing constructor
     */
    public List<Class<?>> getArgumentTypes() {
        return argumentTypes;
    }

    /**
     * The class from which the constructor is missing
     * @return the class from which the constructor is missing
     */
    public Class<?> getSourceType() {
        return sourceType;
    }



}
