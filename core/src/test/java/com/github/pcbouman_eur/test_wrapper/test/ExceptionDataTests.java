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
package com.github.pcbouman_eur.test_wrapper.test;

import com.github.pcbouman_eur.test_wrapper.exception.*;
import com.github.pcbouman_eur.test_wrapper.test.impl.CorrectImplementation;
import com.github.pcbouman_eur.test_wrapper.test.impl.EmptyClass;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ExceptionDataTests {

    private final static boolean[] BOOLEANS = {false, true};
    private final static List<Class<?>> TYPES = Arrays.asList(String.class, BigInteger.class);
    private final static List<Class<?>> CLASSES = Arrays.asList(CorrectImplementation.class, EmptyClass.class);
    private final static List<List<Class<?>>> ARG_TYPES = Arrays.asList(
            Collections.emptyList(),
            Collections.singletonList(int.class),
            Collections.singletonList(String.class),
            Arrays.asList(String.class, int.class),
            Arrays.asList(String.class, double.class, List.class)
    );
    private final static List<String> FIELD_NAMES = Arrays.asList("test1", "test2", "number", "flag");
    private final static List<String> METHOD_NAMES = Arrays.asList("doSomething", "run", "sayHI", "print");

    private final Field[] testFields;
    private final Method[] methods;

    public ExceptionDataTests() throws NoSuchFieldException {
         testFields = new Field[] {
                 CorrectImplementation.class.getField("NUMBER"),
                 CorrectImplementation.class.getField("MY_LIST")
         };
         methods = CorrectImplementation.class.getMethods();
    }

    @Test
    public void testDataPresent() {
        assertTrue(testFields.length > 0);
        assertTrue(methods.length > 0);
    }

    @Test
    public void testFieldStateException() {
        for (Field testField : testFields) {
            for (boolean b : BOOLEANS) {
                FieldStateException fse = new FieldStateException(testField, b);
                assertEquals(fse.getField(), testField);
                assertEquals(fse.isShouldBeStatic(), b);
            }
        }
    }

    @Test
    public void testFieldTypeException() {
        for (Field testField : testFields) {
            for (Class<?> type : TYPES) {
                FieldTypeException fte = new FieldTypeException(testField, type);
                assertEquals(fte.getField(), testField);
                assertEquals(fte.getExpectedType(), type);
            }
        }
    }

    @Test
    public void testMethodStateException() {
        for (Method m : methods) {
            MethodStateException mse = new MethodStateException(m);
            assertEquals(mse.getMethod(), m);
        }
    }

    @Test
    public void testMissingConstructorException() {
        for (List<Class<?>> argTypes : ARG_TYPES) {
            for (Class<?> clz : CLASSES) {
                MissingConstructorException mce = new MissingConstructorException(argTypes, clz);
                assertEquals(argTypes, mce.getArgumentTypes());
                assertEquals(clz, mce.getSourceType());
            }
        }
    }

    @Test
    public void testMissingFieldException() {
        for (String fieldname : FIELD_NAMES) {
            for (Class<?> clz : CLASSES) {
                for (Class<?> type : TYPES) {
                    for (boolean b : BOOLEANS) {
                        MissingFieldException mfe = new MissingFieldException(fieldname, clz, type, b);
                        assertEquals(fieldname, mfe.getFieldName());
                        assertEquals(clz, mfe.getSourceClass());
                        assertEquals(type, mfe.getExpectedType());
                        assertEquals(b, mfe.isStaticField());
                    }
                }
            }
        }
    }

    @Test
    public void testMissingMethodException() {
        for (Method m : methods) {
            for (Class<?> clz : CLASSES) {
                MissingMethodException mme = new MissingMethodException(m, clz);
                assertEquals(m, mme.getMethod());
                assertEquals(clz, mme.getClz());
            }
        }
    }

    @Test void MissingStaticMethodException() {
        for (Class<?> clz : CLASSES) {
            for (String methodName : METHOD_NAMES) {
                for (Class<?> type : TYPES) {
                    for (List<Class<?>> argTypes : ARG_TYPES) {
                        MissingStaticMethodException msme;
                        msme = new MissingStaticMethodException(clz, methodName, type, argTypes);
                        assertEquals(clz, msme.getSourceType());
                        assertEquals(methodName, msme.getMethodName());
                        assertEquals(type, msme.getReturnType());
                        assertEquals(argTypes, msme.getArgTypes());
                    }
                }
            }
        }

    }

}
