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
import com.github.pcbouman_eur.test_wrapper.test.impl.*;
import com.github.pcbouman_eur.test_wrapper.WrapperFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class WrapperTests {

    private final WrapperFactory<TargetInterface, CorrectImplementation> fac1
            = new WrapperFactory<>(TargetInterface.class, CorrectImplementation.class);

    private final WrapperFactory<TargetInterface, EmptyClass> fac2
            = new WrapperFactory<>(TargetInterface.class, EmptyClass.class);

    private final WrapperFactory<ExceptionTestInterface, ExceptionGenerator> fac3
            = new WrapperFactory<>(ExceptionTestInterface.class, ExceptionGenerator.class);

    @Test
    void testConstructors() {
        // Create some objects by calling constructors on the wrapper factories
        // We store the expected class of the wrapped objects created by the wrappers
        Map<TargetInterface, Class<?>> checks =
                Map.of(
                        fac1.constructor(), CorrectImplementation.class,
                        fac1.constructor(5, List.of("x", "y")), CorrectImplementation.class,
                        fac2.constructor(), EmptyClass.class
                );
        for (Map.Entry<TargetInterface, Class<?>> e : checks.entrySet()) {
            TargetInterface ti = e.getKey();
            Class<?> sourceClass = e.getValue();
            assertNotNull(ti, "Calling a constructor using the factory should return an object");
            assertTrue(sourceClass.isInstance(fac1.unwrap(ti)),
                    "Unwrapping an object created by the wrapper factory should give an object of the" +
                            "source class");
        }
    }

    @Test
    void testRawConstructors() {
        // Create an unwrapped object
        CorrectImplementation ci = fac1.rawConstructor(36, List.of("a", "b"));
        // Call the copy constructor using the unwrapped object
        TargetInterface copy = fac1.constructor(ci);
        TargetInterface w1 = fac1.wrapToResult(ci);
        compareObjects(copy, w1, "Check if the copy constructor copies the object correctly");
    }

    @Test
    void testStaticFactory() {
        TargetInterface ti = fac1.invokeStaticFactory("createStatic");
        CorrectImplementation ci = CorrectImplementation.createStatic();
        String msg = "Check if a static factory produces the same object as calling the static method directly";
        compareObjects(ti, ci, msg);
    }

    @Test
    void testWrapUnwrap() {
        // Directly instantiate an object.
        // Don't do this in actual test code as it will lead to compilation errors for students
        CorrectImplementation ci = new CorrectImplementation();
        TargetInterface ti = fac1.wrapToResult(ci);
        assertSame(fac1.unwrap(ti), ci, "Wrapping and then unwrapping gives the same object");
        EmptyClass empty = new EmptyClass();
        ti = fac2.wrapToResult(empty);
        assertSame(fac2.unwrap(ti), empty, "Wrapping and then unwrapping gives the same object");
    }

    @Test
    void wrappedStateTest() {
        // Repeat this five times to detect potential issues with the method cache of the WrapperFactory
        for (int t=0; t < 5; t++) {
            CorrectImplementation ci = new CorrectImplementation();
            TargetInterface ti = fac1.constructor();
            compareObjects(ti, ci, "Compare initial states of directly and indirectly objects");
            ci.setFlagToTrue();
            ti.setFlagToTrue();
            compareObjects(ti, ci, "Compare states after toggling flag");
            ci.setBoxed(20);
            ti.setBoxed(20);
            compareObjects(ti, ci, "Compare states after using setBoxed");
            ci.setPrimitive(25);
            ti.setPrimitive(25);
            compareObjects(ti, ci, "Compare states after using setPrimitive");
            ci.addToList("test1", "test2", "zzzz");
            ti.addToList("test1", "test2", "zzzz");
            compareObjects(ti, ci, "Compare states after using addToList");
        }
    }

    @Test
    void staticMethodTest() {
        // Check if calling a static method through the factory yields the same result as calling it directly
        assertEquals(CorrectImplementation.staticMethod("ab", 5),
                     fac1.invokeStatic("staticMethod", String.class, "ab", 5));
        List<String> testList = new ArrayList<>();
        fac1.invokeStatic("staticAddToList", testList, "hi", 12);
        assertEquals(testList.size(), 12,
                "Successfully invoke a static method that has void as it's return type");
        Assertions.assertThrows(MethodStateException.class,
                () -> fac1.invokeStatic("addToList", "A", "B", "C"),
                "A MethodStateException is thrown if a non-static method is called in a static way");
    }

    @Test
    void testFieldAccess() {
        // Repeat this five time to detect potential issues with the cache of the WrapperFactory
        for (int t=0; t < 5; t++) {
            // Testing reading/writing access to a static field
            String message = fac1.readField("GLOBAL_MESSAGE", String.class);
            assertEquals(message, CorrectImplementation.GLOBAL_MESSAGE, "Check if the wrapper can read" +
                    " a static field");
            String testMsg = "A different message";
            fac1.setField("GLOBAL_MESSAGE", String.class, testMsg);
            assertEquals(testMsg, CorrectImplementation.GLOBAL_MESSAGE, "Check if the wrapper can set" +
                    " a static field");
            CorrectImplementation.GLOBAL_MESSAGE = message;

            // Testing reading/writing access to an object field
            TargetInterface ti = fac1.constructor();
            ti.setPrimitive(50);
            assertEquals(50, fac1.readField("number", int.class, ti), "An object field can be " +
                    "read using the WrapperFactory");
            fac1.setField("number", int.class, ti, 25);
            assertEquals(25, ti.getPrimitive(), "An object field can be " +
                    "set through the WrapperFactory");
        }
    }

    @Test
    void testExpectedMissingExceptions() {
        // First check if we do get MissingConstructorExceptions
        Assertions.assertThrows(MissingConstructorException.class,
                () -> fac2.constructor(5, List.of("x", "y")),
                "Check if the empty class indeed misses a non-empty constructor");
        // Now instantiate an object of the empty class with the empty constructor
        TargetInterface ti = fac2.constructor();
        Assertions.assertThrows(MissingMethodException.class,
                ti::setFlagToTrue, "Check if the empty classes indeed misses" +
                        " a method from the interface");
        Assertions.assertThrows(MissingStaticMethodException.class,
                () -> fac2.invokeStatic("staticMethod", String.class, "ab", 5),
                "Check if the empty class indeed misses the static method");
    }

    @Test
    void testUnwrappingRuntimeException() {
        TargetInterface ti = fac1.constructor();
        assertThrows(IllegalArgumentException.class,
                () -> ti.giveMePositive(-5),
                "Check if runtime exceptions that occur in student code are properly unpacked by the wrapper");
    }

    @Test
    void testFieldExceptions() {
        TargetInterface ti = fac1.constructor();
        // Exception on static read/write on a non-static field
        Assertions.assertThrows(FieldStateException.class,
                () -> fac1.readField("number", int.class),
                "A FieldStateException should be thrown if a non-static field is read in a static way");
        assertThrows(FieldStateException.class,
                () -> fac1.setField("number", int.class, 12),
                "A FieldStateException should be thrown if a non-static field is set in a static way");
        // Exception on non-static read/write on a static field
        assertThrows(FieldStateException.class,
                () -> fac1.readField("GLOBAL_MESSAGE", String.class, ti),
                "A FieldStateException should be thrown if a static field is read in a non-static way");
        assertThrows(FieldStateException.class,
                () -> fac1.setField("GLOBAL_MESSAGE", String.class, ti, "New message"),
                "A FieldStateException should be thrown if a static field is set in a non-static way");

        // Exception on incorrect field type
        assertThrows(FieldTypeException.class,
                () -> fac1.readField("GLOBAL_MESSAGE", int.class),
                "A FieldTypeException should be thrown if a static field is read for the wrong type");
        assertThrows(FieldTypeException.class,
                () -> fac1.setField("GLOBAL_MESSAGE", int.class, 12),
                "A FieldTypeException should be thrown if a static field is set for the wrong type");
        assertThrows(FieldTypeException.class,
                () -> fac1.readField("number", String.class, ti),
                "A FieldTypeException should be thrown if a non-static field is read for the wrong type");
        assertThrows(FieldTypeException.class,
                () -> fac1.setField("number", String.class, ti, "New message"),
                "A FieldTypeException should be thrown if a non-static field is set for the wrong type");

        // Exception missing static field / missing non-static field
        TargetInterface ti2 = fac2.constructor();
        assertThrows(MissingFieldException.class,
                () -> fac2.readField("GLOBAL_MESSAGE", String.class),
                "A MissingFieldException should be thrown if a non-existing static field is read");
        assertThrows(MissingFieldException.class,
                () -> fac2.readField("number", int.class, ti2),
                "A MissingFieldException should be thrown if a non-existing field is read");
        assertThrows(MissingFieldException.class,
                () -> fac2.setField("GLOBAL_MESSAGE", String.class, "New message"),
                "A MissingFieldException should be thrown if a non-existing static field is set");
        assertThrows(MissingFieldException.class,
                () -> fac2.setField("number", int.class, 12),
                "A MissingFieldException should be thrown if a non-existing field is set");
    }

    @Test
    void testCheckedExceptionThrowingCode() throws Throwable {
        // Check if Checked Exception are properly passed on by the ex methods and proxy wrapper
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                fac3::constructorEx,
                "A TestCheckedException is thrown directly by the constructorEx method");
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                fac3::rawConstructorEx,
                "A TestCheckedException is thrown directly by the rawConstructorEx method");
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                () -> fac3.invokeStaticEx("doSomethingStatic"),
                "A TestCheckedException is thrown directly by the invokeStaticEx method");
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                () -> fac3.invokeStaticEx("getSomethingStatic", String.class),
                "A TestCheckedException is thrown directly by the invokeStaticEx method");
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                () -> fac3.invokeStaticFactoryEx("create"),
                "A TestCheckedException is thrown directly by the invokeStaticFactoryEx method");
        ExceptionTestInterface eti = fac3.constructorEx(false);
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                eti::getSomething,
                "A TestCheckedException is thrown directly through the proxy wrapped");
        assertThrows(ExceptionGenerator.TestCheckedException.class,
                eti::doSomething,
                "A TestCheckedException is thrown directly through the proxy wrapped");

        // Check if the non-Ex versions of the methods throw a WrappedException
        assertThrows(WrappedException.class,
                fac3::constructor,
                "A WrappedException is thrown directly by the constructor method");
        assertThrows(WrappedException.class,
                fac3::rawConstructor,
                "A WrappedException is thrown directly by the rawConstructor method");
        assertThrows(WrappedException.class,
                () -> fac3.invokeStatic("doSomethingStatic"),
                "A WrappedException is thrown directly by the invokeStatic method");
        assertThrows(WrappedException.class,
                () -> fac3.invokeStatic("getSomethingStatic", String.class),
                "A WrappedException is thrown directly by the invokeStatic method");
        assertThrows(WrappedException.class,
                () -> fac3.invokeStaticFactory("create"),
                "A WrappedException is thrown directly by the invokeStaticFactory method");

        // These calls should NOT throw an Exception but run normally
        assertEquals(CorrectImplementation.staticMethod("ab", 5),
                fac1.invokeStaticEx("staticMethod", String.class, "ab", 5));
        List<String> testList = new ArrayList<>();
        fac1.invokeStaticEx("staticAddToList", testList, "hi", 12);
        assertEquals(testList.size(), 12,
                "Successfully invoke a static method that has void as it's return type");
        TargetInterface ti = fac1.invokeStaticFactoryEx("createStatic");
        CorrectImplementation ci = CorrectImplementation.createStatic();
        String msg = "Check if a static factory produces the same object as calling the static method directly";
        compareObjects(ti, ci, msg);

        CorrectImplementation ci2 = fac1.rawConstructorEx(36, List.of("a", "b"));
        // Call the copy constructor using the unwrapped object
        TargetInterface copy = fac1.constructor(ci2);
        TargetInterface w1 = fac1.wrapToResult(ci2);
        compareObjects(copy, w1, "Check if the copy constructor copies the object correctly");
    }


    void compareObjects(TargetInterface ti, CorrectImplementation ci, String msg) {
        assertEquals(ti.getFlag(), ci.getFlag(), msg);
        assertEquals(ti.getPrimitive(), ci.getPrimitive(), msg);
        assertEquals(ci.getBoxed(), ci.getBoxed(), msg);
        assertEquals(ti.getCurrentList(), ci.getCurrentList(), msg);
    }

    void compareObjects(TargetInterface ti1, TargetInterface ti2, String msg) {
        assertEquals(ti1.getFlag(), ti2.getFlag(), msg);
        assertEquals(ti1.getPrimitive(), ti2.getPrimitive(), msg);
        assertEquals(ti1.getBoxed(), ti2.getBoxed(), msg);
        assertEquals(ti1.getCurrentList(), ti2.getCurrentList(), msg);
    }

}
