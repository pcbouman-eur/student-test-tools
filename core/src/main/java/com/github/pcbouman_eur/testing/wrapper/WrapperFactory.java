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

package com.github.pcbouman_eur.testing.wrapper;

import com.github.pcbouman_eur.testing.wrapper.exception.*;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>The purpose of this class is to make access to classes written by students indirect in testing code.
 * The advantage of this is that if a student forgets to implement a certain method, compilation will not
 * fail in the test code, because the method is only accessed indirectly via a proxy object.</p>
 *
 * <p>The WrapperFactory provides specialized methods to indirectly call constructors and static methods on the
 * student's class. This also means that even constructors and static methods, which can not be defined in an
 * interface, can be used in testcases without the compiler failing in case a student forget to implement them.</p>
 *
 * <p>When used properly, all access to the student's code will happen through reflection. This means that if a
 * student has not implemented particular methods/constructors, a specific RuntimeException will be thrown.
 * For example a MissingMethodException or MissingConstructorException. These exceptions provide better feedback
 * to the student than cases where testcases directly access their classes. Direct compilation of test classes
 * against student classes typically give a missing symbol error in the test code, which is confusing to the
 * student as they did not write that code.</p>
 *
 * @param <I> the target interface that contains the methods a student is supposed to implement
 * @param <S> the source class a student should implement and contain the methods defined in the
 *            interface. However, the class does not have to implement the interface itself.
 */
public final class WrapperFactory<I,S> {

    private final Class<I> targetInterface;
    private final Class<S> studentClass;
    private final Map<Method,Method> methodMap;
    private final Map<List<Class<?>>,Constructor<S>> constructorMap;
    private final Map<String,Field> staticFieldMap;
    private final Map<String,Field> fieldMap;

    /**
     * <p>Constructor to create a WrapperFactory object.</p>
     *
     * <p>It is advisable to construct only one factory within a Unit Test class, and not a new factory for each
     * test method, as the WrapperFactory maintains a cache of methods for faster access.</p>
     *
     * @param targetInterface the class object associated with the target interface that the student should implement
     * @param studentClass the class object associated with the class the student is supposed to implement
     */
    public WrapperFactory(Class<I> targetInterface, Class<S> studentClass) {
        this.targetInterface = targetInterface;
        this.studentClass = studentClass;
        methodMap = new LinkedHashMap<>();
        constructorMap = new LinkedHashMap<>();
        staticFieldMap = new LinkedHashMap<>();
        fieldMap = new LinkedHashMap<>();
    }

    /**
     * Creates a wrapped object that has the type of the target interface I, and which delegates all method calls
     * to calls of methods on the source methods, if those methods exists in classes of type S. If they do not
     * exist, a MissingMethodException is thrown when such a method is called
     *
     * @param source and object of the student class S that should be wrapped
     * @return a wrapper of type I that delegates methods to the source object
     */
    @SuppressWarnings("unchecked")
    public I wrapToResult(S source) {
        Wrapper f = new Wrapper(source);
        return (I) Proxy.newProxyInstance(targetInterface.getClassLoader(), new Class<?>[]{targetInterface}, f);
    }

    /**
     * Method that can be used to call a static factory method of the student class S and wraps the result in a
     * wrapper object
     *
     * @param methodName the name of the static method to call
     * @param args the arguments to pass into the static method
     * @return a wrapped object of type I that delegates calls to the methods of the object obtained using the
     *         static factory method
     * @throws MissingStaticMethodException if the factory method does not exist on a class of type S
     * @throws WrappedException if the factory method produced a checked exception, it is wrapped into this
     */
    public I invokeStaticFactory(String methodName, Object... args) throws MissingStaticMethodException,
            WrappedException {
        return wrapToResult(invokeStatic(methodName, studentClass, args));
    }




    /**
     * Calls a static method on the student class and returns the result returned by that method.
     *
     * @param methodName the name of the static method to call on student class S
     * @param returnType a Class representing the return type of the method
     * @param args the arguments to pass to the method
     * @return the object returned by the static method
     * @param <E> the return type of the static method to be called
     * @throws MissingStaticMethodException if the static method does not exist in student class S
     * @throws MethodStateException if the method can be found, but is a non-static method
     * @throws WrappedException if the called method threw a checked Exception it is wrapped and thrown
     */
    @SuppressWarnings("unchecked")
    public <E> E invokeStatic(String methodName, Class<E> returnType, Object... args)
            throws MissingStaticMethodException, MethodStateException, WrappedException{
        try {
            Method m = findStaticMethod(methodName, returnType, args);
            try {
                return (E) m.invoke(null, args);
            } catch (IllegalAccessException e) {
                throw new AssertionError("Reflective access is disabled: "+e.getMessage());
            } catch (InvocationTargetException ex) {
                WrappedException.safeThrowHelper(ex);
                throw new AssertionError("This should be unreachable code as safeThrowHelper should throw");
            }
        } catch (SecurityException e) {
            throw new AssertionError("Reflective access is disabled: "+e.getMessage());
        }
    }


    /**
     * Call a static method that has a void return type
     *
     * @param methodName the name of the static void method to call on student class S
     * @param args the arguments to pass to the void method
     * @throws MissingStaticMethodException if the static method does not exist in student class S
     * @throws MethodStateException if the method can be found, but is a non-static method
     * @throws WrappedException if the called method threw a checked Exception it is wrapped and thrown
     */
    public void invokeStatic(String methodName, Object... args)
            throws MissingStaticMethodException, MethodStateException, WrappedException {
        invokeStatic(methodName, void.class, args);
    }

    /**
     * Method that can be used to call a static factory method of the student class S and wraps the result in a
     * wrapper object. Allows checked exceptions to be thrown.
     *
     * Can produce unwrapped checked exceptions, and should probably be only be used to test if the student correctly
     * throws checked exceptions.
     *
     * @param methodName the name of the static method to call
     * @param args the arguments to pass into the static method
     * @return a wrapped object of type I that delegates calls to the methods of the object obtained using the
     *         static factory method
     * @throws MissingStaticMethodException if the factory method does not exist on a class of type S
     * @throws Throwable any potential exception that was thrown by the factory method
     */
    public I invokeStaticFactoryEx(String methodName, Object... args) throws MissingStaticMethodException, Throwable {
        return wrapToResult(WrappedException.getOrThrow(() -> invokeStatic(methodName, studentClass, args)));
    }

    /**
     * Calls a static method on the student class and returns the result returned by that method.
     * Allows checked exceptions to be thrown.
     *
     * Can produce unwrapped checked exceptions, and should probably be only be used to test if the student correctly
     * throws checked exceptions.
     *
     * @param methodName the name of the static method to call on student class S
     * @param returnType a Class representing the return type of the method
     * @param args the arguments to pass to the method
     * @return the object returned by the static method
     * @param <E> the return type of the static method to be called
     * @throws MissingStaticMethodException if the static method does not exist in student class S
     * @throws MethodStateException if the method can be found, but is a non-static method
     * @throws Throwable any potential exception that was thrown by the factory method
     */
    public <E> E invokeStaticEx(String methodName, Class<E> returnType, Object... args)
            throws MissingStaticMethodException, MethodStateException, Throwable{
        return WrappedException.getOrThrow(() -> invokeStatic(methodName, returnType, args));
    }

    /**
     * Call a static method that has a void return type
     * Allows checked exceptions to be thrown.
     *
     * Can produce unwrapped checked exceptions, and should probably be only be used to test if the student correctly
     * throws checked exceptions.
     *
     * @param methodName the name of the static void method to call on student class S
     * @param args the arguments to pass to the void method
     * @throws MissingStaticMethodException if the static method does not exist in student class S
     * @throws MethodStateException if the method can be found, but is a non-static method
     * @throws Throwable any potential exception that was thrown by the factory method
     */
    public void invokeStaticEx(String methodName, Object... args)
            throws MissingStaticMethodException, MethodStateException, Throwable {
        WrappedException.doOrThrow(() -> invokeStatic(methodName, void.class, args));
    }


    /**
     * Calls a constructor on the student class S and wraps the resulting object in a wrapper object
     * which implements interface I. Should be used in tests to construct objects rather than calling the constructor
     * of the student class directly.
     *
     * @param args the arguments to pass into the constructor
     * @return a wrapper of type I around an object that was created by a constructor of the student class S
     * @throws MissingConstructorException if student class S has no constructor that matches the arguments
     * @throws WrappedException if the called constructor threw a checked Exception it is wrapped and thrown
     */
    public I constructor(Object ... args) throws MissingConstructorException, WrappedException {
        return wrapToResult(rawTypedConstructor(typeArray(args), args));
    }

    /**
     * Calls a constructor on the student class S and return the resulting object
     * If the constructor does not exist, a MissingConstructorException is thrown
     *
     * @param args the arguments to pass into the constructor
     * @return a newly created object of the student class S
     * @throws MissingConstructorException if student class S has no constructor that matches the arguments
     * @throws WrappedException if the called constructor threw a checked Exception it is wrapped and thrown
     */
    public S rawConstructor(Object ... args) throws MissingConstructorException, WrappedException {
        return rawTypedConstructor(typeArray(args), args);
    }

    /**
     * Calls a constructor on the student class S and wraps the resulting object in a wrapper object
     * which implements interface I. Should be used in tests to construct objects rather than calling the constructor
     * of the student class directly.
     *
     * Can produce unwrapped checked exceptions, and should probably be only be used to test if the student correctly
     * throws checked exceptions.
     *
     * @param args the arguments to pass into the constructor
     * @return a wrapper of type I around an object that was created by a constructor of the student class S
     * @throws MissingConstructorException if student class S does not implement a constructor that matches the arguments
     * @throws Throwable any potential exception that was thrown by the constructor
     */
    public I constructorEx(Object ... args) throws MissingConstructorException, Throwable {
        return wrapToResult(WrappedException.getOrThrow(() -> rawTypedConstructor(typeArray(args), args)));
    }

    /**
     * Calls a constructor on the student class S and return the resulting object
     * If the constructor does not exist, a MissingConstructorException is thrown
     *
     * Can produce unwrapped checked exceptions, and should probably be only be used to test if the student correctly
     * throws checked exceptions.
     *
     * @param args the arguments to pass into the constructor
     * @return a newly created object of the student class S
     * @throws MissingConstructorException if student class S has no constructor that matches the arguments
     * @throws Throwable any potential exception that was thrown by the constructor
     */
    public S rawConstructorEx(Object ... args) throws MissingConstructorException, Throwable {
        return WrappedException.getOrThrow(() -> rawTypedConstructor(typeArray(args), args));
    }

    private S rawTypedConstructor(Class<?>[] argumentTypes, Object... args) {
        List<Class<?>> typeList = Arrays.asList(argumentTypes);
        Constructor<S> cons = constructorMap.get(typeList);
        if (cons == null) {
            cons = ConstructorUtils.getMatchingAccessibleConstructor(studentClass, argumentTypes);
            if (cons == null) {
                throw new MissingConstructorException(Arrays.asList(argumentTypes), studentClass);
            }
            constructorMap.put(typeList, cons);
        }

        try {
            return cons.newInstance(args);
        } catch (InstantiationException e) {
            throw new AssertionError("Unexpected InstantiationException: "+e.getMessage());
        } catch (IllegalAccessException e) {
            throw new AssertionError("IllegalAccess: "+e.getMessage());
        } catch (InvocationTargetException ex) {
            WrappedException.safeThrowHelper(ex);
            throw new AssertionError("This should never occur as safeThrowHelper should always throw");
        }
    }

    /**
     * Extract the raw object of student class S from a wrapper object implementing interface I
     * @param wrappedObj a wrapped object created with this WrapperFactory
     * @return the unwrapped object of the student class S
     * @throws IllegalArgumentException if the provided object is not a Wrapper created by this WrapperFactory
     */
    @SuppressWarnings("unchecked")
    public S unwrap(I wrappedObj) throws IllegalArgumentException {
        if (studentClass.isInstance(wrappedObj)) {
            return (S) wrappedObj;
        }
        InvocationHandler handler = Proxy.getInvocationHandler(wrappedObj);
        if (handler instanceof WrapperFactory.Wrapper) {
            Wrapper w = (Wrapper) handler;
            return w.getSource();
        }
        throw new IllegalArgumentException("The argument was not wrapped by this WrapperFactory");
    }

    /**
     * Reads a static field from the student class S
     * @param fieldname the name of the field
     * @param fieldType the type of the field
     * @return the data currently stored in the static field
     * @param <T> the type of the field
     * @throws MissingFieldException if the field is not defined in the student class S
     * @throws FieldStateException if the field is non-static
     * @throws FieldTypeException if the type of the field in student class S does not match the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T readField(String fieldname, Class<T> fieldType)
            throws MissingFieldException, FieldStateException, FieldTypeException {
        Field f = getField(fieldname, fieldType, true);
        return (T) readField(f, null);
    }

    /**
     * Reads a field from an object of the student class S
     * @param fieldname the name of the field
     * @param fieldType the type of the field
     * @param obj the wrapper of the object from which the field should be read
     * @return the data currently stored in the static field
     * @param <T> the type of the field
     * @throws MissingFieldException if the field is not defined in the student class S
     * @throws FieldStateException if the field is static
     * @throws FieldTypeException if the type of the field in student class S does not match the expected type
     */
    @SuppressWarnings("unchecked")
    public <T> T readField(String fieldname, Class<T> fieldType, I obj)
            throws MissingFieldException, FieldStateException, FieldTypeException {
        Field f = getField(fieldname, fieldType, false);
        return (T) readField(f, unwrap(obj));
    }

    /**
     * Store a value in a static field from the student class S
     * @param fieldname the name of the field
     * @param fieldType the type of the field
     * @param value the value to store in the static field
     * @param <T> the type of the field
     * @throws MissingFieldException if the field is not defined in the student class S
     * @throws FieldStateException if the field is non-static
     * @throws FieldTypeException if the type of the field in student class S does not match the expected type
     */
    public <T> void setField(String fieldname, Class<T> fieldType, T value)
            throws MissingFieldException, FieldStateException, FieldTypeException {
        Field f = getField(fieldname, fieldType, true);
        setField(f, null, value);
    }

    /**
     * Store a value in a field of an object of the student class S
     * @param fieldname the name of the field
     * @param fieldType the type of the field
     * @param object the wrapper of the object from which the field should be read
     * @param value the value to store in the object field
     * @param <T> the type of the field
     * @throws MissingFieldException if the field is not defined in the student class S
     * @throws FieldStateException if the field is static
     * @throws FieldTypeException if the type of the field in student class S does not match the expected type
     */
    public <T> void setField(String fieldname, Class<T> fieldType, I object, T value)
            throws MissingFieldException, FieldStateException, FieldTypeException {
        Field f = getField(fieldname, fieldType, false);
        setField(f, unwrap(object), value);
    }

    private Object readField(Field f, Object obj) {
        try {
            boolean acc = f.canAccess(obj);
            if (!acc) {
                f.setAccessible(true);
            }
            Object value = f.get(obj);
            if (!acc) {
                f.setAccessible(false);
            }
            return value;
        }
        catch (IllegalAccessException e) {
            throw new AssertionError("Reflective access is disabled: "+e.getMessage(), e);
        }
    }

    private void setField(Field f, Object obj, Object value) {
        try {
            boolean acc = f.canAccess(obj);
            if (!acc) {
                f.setAccessible(true);
            }
            f.set(obj, value);
            if (!acc) {
                f.setAccessible(false);
            }
        }
        catch (IllegalAccessException e) {
            throw new AssertionError("Reflective access is disabled: "+e.getMessage(), e);
        }
    }

    private Method findStaticMethod(String methodName, Class<?> returnType, Object... args) {
        Class<?>[] parameterTypes = typeArray(args);
        Method method = MethodUtils.getMatchingAccessibleMethod(studentClass, methodName, parameterTypes);
        if (method == null)  {
            throw new MissingStaticMethodException(studentClass, methodName, returnType, Arrays.asList(parameterTypes));
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new MethodStateException(method);
        }
        boolean isVoid = returnType == void.class && method.getReturnType() == void.class;
        if (isVoid || returnType.isAssignableFrom(method.getReturnType())) {
            return method;
        }
        throw new MissingStaticMethodException(studentClass, methodName, returnType, Arrays.asList(parameterTypes));
    }

    private Field getField(String fieldname, Class<?> fieldType, boolean staticField) {
        Field result = staticField ? staticFieldMap.get(fieldname) : fieldMap.get(fieldname);
        if (result != null) {
            return result;
        }
        try {
            result = studentClass.getDeclaredField(fieldname);
        }
        catch (NoSuchFieldException ex) {
            // Empty as we will also make an attempt using the .getField() method
        }
        if (result == null) {
            try {
                result = studentClass.getField(fieldname);
            } catch (NoSuchFieldException ex) {
                throw new MissingFieldException(fieldname, studentClass, fieldType, staticField);
            }
        }
        if (Modifier.isStatic(result.getModifiers()) != staticField) {
            throw new FieldStateException(result, staticField);
        }
        if (!result.getType().isAssignableFrom(fieldType)) {
            throw new FieldTypeException(result, fieldType);
        }
        if (staticField) {
            staticFieldMap.put(fieldname, result);
        }
        else {
            fieldMap.put(fieldname, result);
        }
        return result;
    }

    private Class<?>[] typeArray(Object... args) {
        Class<?>[] result = new Class[args.length];
        for (int i=0; i < args.length; i++) {
            Object o = args[i];
            if (o == null) {
                result[i] = Object.class;
            } else {
                result[i] = o.getClass();
            }
        }
        return result;
    }

    /**
     * Simple wrapper around a source object of the student class.
     * It implements InvocationHandler, using a simple delegation strategy.
     * If a method is called on this proxy, the handler tries to find the corresponding method in the
     * student class, and then calls that method for a delegate call. If no corresponding method can be
     * found in the student class, a MissingMethodException is thrown
     */
    protected final class Wrapper implements InvocationHandler {

        private final S source;

        /**
         * Create a wrapper around a particular object of the student class
         * @param source the object to wrap
         */
        protected Wrapper(S source) {
            this.source = source;
        }

        /**
         * The object wrapped by this wrapper
         * @return the wrapped object
         */
        public S getSource() {
            return source;
        }

        /**
         * Implementation of the InvocationHandler interface. When a method of the
         * target interface I is called on the proxy object, this wrapped handles that call
         * by looking for a matching method in the Student class. If such a method exists,
         * it is called and the result is returned. If an exception occurs during the call,
         * there are two options: if it is an unchecked exception, the exception is thrown
         * by this method. If it is a checked exception, it is wrapped in a WrappedException
         * object, which can then be used to access the particular checked exception.
         *
         * @param obj the proxy instance that the method was invoked on
         *
         * @param method the {@code Method} instance corresponding to
         * the interface method invoked on the proxy instance.  The declaring
         * class of the {@code Method} object will be the interface that
         * the method was declared in, which may be a superinterface of the
         * proxy interface that the proxy class inherits the method through.
         *
         * @param args an array of objects containing the values of the
         * arguments passed in the method invocation on the proxy instance,
         * or {@code null} if interface method takes no arguments.
         * Arguments of primitive types are wrapped in instances of the
         * appropriate primitive wrapper class, such as
         * {@code java.lang.Integer} or {@code java.lang.Boolean}.
         *
         * @return the value returned by the delegated method
         * @throws MissingMethodException if no matching method was found in the Student class
         * @throws RuntimeException if during the execution of the method an unchecked exception occurs
         * @throws Throwable if during the execution of the method an exception occurs
         */
        public Object invoke(Object obj, Method method, Object [] args) throws MissingMethodException,
                RuntimeException, Throwable {
            Method sourceMethod = methodMap.get(method);
            if (sourceMethod == null) {
                try {
                    Method target = studentClass.getMethod(method.getName(), method.getParameterTypes());
                    sourceMethod = target;
                    methodMap.put(method, target);
                } catch (NoSuchMethodException e) {
                    throw new MissingMethodException(method, studentClass);
                } catch (SecurityException e) {
                    throw new AssertionError("Reflective access is disabled: "+e.getMessage(), e);
                }
            }
            try {
                return sourceMethod.invoke(source, args);
            } catch (IllegalAccessException e) {
                throw new AssertionError("Reflective access is disabled: "+e.getMessage(), e);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }

        }
    }

}
