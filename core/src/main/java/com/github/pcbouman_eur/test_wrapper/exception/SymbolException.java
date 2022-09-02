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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base class for many code structure related exception that provides some helper methods to format types in
 * error messages
 */
public class SymbolException extends RuntimeException {

    /**
     * Constructor that store an error message
     * @param message the error message of this exception
     */
    public SymbolException(String message) {
        super(message);
    }

    /**
     * Helper method to layout a list of types
     * @param types a list of types that should be presented to a student
     * @return a list with the types as strings, presented in a simpler format
     */
    protected static List<String> convertTypes(List<Class<?>> types) {
        return types.stream()
                .map(SymbolException::convertType)
                .collect(Collectors.toList());
    }

    /**
     * Layouts a single type so it can be understood easily by a student
     * @param type an object representing the type to format
     * @return a simple, understable representation of a type to be understood by a student
     */
    protected static String convertType(Class<?> type) {
        if (type.isArray()) {
            String subType = convertType(type.getComponentType());
            return subType + " []";
        }
        return type.getSimpleName();
    }

}
