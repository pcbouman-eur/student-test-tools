# Student Test Tools

For my teaching in the course FEB22012X Programming, I rely heavily on the
[Codegrade](https://www.codegrade.com/) application to provide automated
feedback to students. The main advantage of this is that students can
keep working on their assignment and fix their mistakes. This way, students
can learn from practice by fixing their mistakes, rather than getting one
shot feedback at the end of their assignment work.

This requires that the teacher writes software that can check and provide
feedback for the student's code. Working with Java, it makes sense to
make use of the popular tooling that was developed for testing Java programs,
of which JUnit is a very popular option. However, there are a couple of drawbacks
working with plain JUnit:

* Assertions that pass successfully are discarded by standard unit testing frameworks. When providing feedback to students, it is valuable and encouraging to provide feedback on things that are correct.
* Standard runners for JUnit tests may result in very long stacktraces of which only a small part is relevant. It can be helpful for students to only show the interesting part.
* Standard unit tests stop as soon as an assertion fails - this makes sense as their main purpose is to warn developers something broke. However, for student it may be useful to communicate if more issues can be found with their code. Splitting each assertion up over a separate test method is not always convenient.
* There is no natural support to define that at least some but not necessarily all test cases should pass. If we want to allow students to choose between different parts of the assignment, this is typically needed.
* Missing methods or mistakes in student code may result in *missing symbol* errors when compiling the test code. As a consequence, students will see compiler errors in code written by the teacher. Some will thus assume the mistake is at the side of the teacher, and not at the side of the student.

In this project, I have developed some tools that can address these issues. Details for each of the modules are discussed below.

## Custom Test Runner and XML Writer

As Codegrade uses the [junitparser](https://pypi.org/project/junitparser/) Python library to
process and communicate test output to students, it is important to make sure the output
of a test runner is compatible with this library. Unfortunately this is not always the
case for the default Jupiter engine that servers as the default engine for JUnit.

To overcome this issue, an alternative test runner is developed in the `testing-lib`
module, which builds to a fat jar that contains all modules in this project.
This `jar` has two ways to run testcases: the `run` mode which can work with any
JUnit testcases, and a `run-choices` mode which requires a configuration class.

The `run` mode provides the most important options to run test cases:

```shell
$ java -jar testing-lib.jar run
Missing required option: '--class=<classNames>'
Usage: test run [-hV] [-ae] [-ao] [-o=<output>] -c=<classNames>
                [-c=<classNames>]... [-d=<dependencies>]... [-t=<tags>]...
Run selected tests and store the results in an output XML file
      -ae, --allowError      Allows students to print to standard error
      -ao, --allowOutput     ALlows students to print to standard out
  -c, --class=<classNames>   Names of classes containing testcases to run
  -d, --dependency=<dependencies>
                             Names of classes that are dependecies which should
                               be loaded before running the test
  -h, --help                 Show this help message and exit.
  -o, --output=<output>      Name of file the output XML should be written to
  -t, --tag=<tags>           Run only tests with these tags
  -V, --version              Print version information and exit.
```

Note that this CLI tool was developed with Codegrade in mind. For example, the `-o`
argument defaults to the environment variable `CG_JUNIT_XML_LOCATION`. A typical
command I use on Codegrade would be:

```bash
java -jar $FIXTURES/testing-lib.jar run -c TestWorld -t creatures -t getcells
```

which results in running all test methods in `TestWorld` that are either tagged
using `@Tag("creatures")` or using `@Tag("getcells")`.

By default, standard output and standard error are suppressed, so calls students
make to `System.out.println()` and `System.err.println()` do not appear in the
output. In case it is desireable to display this to students, the `-ao` and `-ae`
flags can be added to the test runner command.

## Choices Runner

If we want to provide the option to let students choose which parts of the assignment
to make, we typically want to run all test cases but consider the result good
enough if at least a number of them pass.

For example, in my final assigments I let students choose which extensions they want
to implement. For a full grade, making only two assignments is sufficient. For each
extension, the unit tests are contained in different test classes, and some of the
extensions have two parts which are separated by test tags.

The `choices` module provides a number of annotation that can be used to define a
configuration of multiple test cases to run separately. The top level annotation
used to define such a configuration is `ChoiceTests`, which has two attributes:
`maximumPoints` and `choices`, where choices is a sequence of `@Choice` annotations
define test runs from which the students can choose how many they want to implement.
A `@Choice` annotation has a `name` attribute that is communicated to the student,
a `steps` attribute that defines which test cases to run, and an optional `points`
attribute. A `@TestStep` annotation has the attribute `testClasses` which is a sequence
of `Class<?>` objects containing the tests to run in that step, and an option attribute
`tags` which can be used to select only some test methods with particular tags.

Below is an example of how I defined a choice configuration in one of my assignments:

```java
import com.github.pcbouman_eur.testing.choices.annotations.Choice;
import com.github.pcbouman_eur.testing.choices.annotations.ChoiceTests;
import com.github.pcbouman_eur.testing.choices.annotations.TestStep;

@ChoiceTests(
        maximumPoints = 2.0,
        choices = {
                @Choice(name="Extension 1 - Random Data",
                        steps = @TestStep(testClasses = TestRandomTools.class)),
                @Choice(name="Extension 2a - Statistics",
                        steps = @TestStep(testClasses = TestStatistics.class, tags={"statistics"})),
                @Choice(name="Extension 2b - Statistics",
                        steps = @TestStep(testClasses = TestStatistics.class, tags={"linearmodel"})),
                @Choice(name="Extension 3a - Plotting",
                        steps = @TestStep(testClasses = TestPlotting.class, tags={"scatter"})),
                @Choice(name="Extension 3b - Plotting",
                        steps = @TestStep(testClasses = TestPlotting.class, tags={"histogram"})),
                @Choice(name="Extension 4a - Excel",
                        steps = @TestStep(testClasses = TestFileTools.class, tags={"read"})),
                @Choice(name="Extension 4b - Excel",
                        steps = @TestStep(testClasses = TestFileTools.class, tags={"write"})),
        }
)
public class TestExtensions {
    // The class itself can be left empty as only the annotation are used
}
```

Note that the `points` attribute of a `@Choice` could be added to award more or less
points for particular choices passed successfully by a student. By default, the number of points for a choice is `1`.
If enough of test cases defined by separate `@Choice` options run correctly such that their points add up
to at least the `maximumPoints` defined, the result of running the test will be considered successful.

In order to run the configuration class as shown above, all we need to do is compile it
together with all other test classes, and run the CLI with the `run-choices` action as follows:

```shell
java -jar $FIXTURES/testing-lib.jar run-choices TestExtensions
```

**Note:** perhaps in a future version I will consider to support JSON or YAML based configuration besides the
annotation driven configuration.

## Stack Trace Sanitizer

When running test cases with the engines provided by JUnit, the stack traces
can become extremely long. For example, a student could have a stack trace
that looks as follows:

```
java.lang.UnsupportedOperationException: This doesn't work

	at com.github.pcbouman_eur.testing.sanitze.tests.SanitizedTestClass$StudentMockClass.doSomethingElse(SanitizedTestClass.java:41)
	at com.github.pcbouman_eur.testing.sanitze.tests.SanitizedTestClass$StudentMockClass.doSomething(SanitizedTestClass.java:37)
	at com.github.pcbouman_eur.testing.sanitze.tests.SanitizedTestClass.testWillFail(SanitizedTestClass.java:30)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:566)
	at org.junit.platform.commons.util.ReflectionUtils.invokeMethod(ReflectionUtils.java:727)
	at org.junit.jupiter.engine.execution.MethodInvocation.proceed(MethodInvocation.java:60)
	... more than 50 lines were omitted for reasons of brevity
```

The module `sanitize` provides a JUnit plugin that can automatically get rid of these long stack traces, cutting them off at the point
where classes not written by the student are reached. For example, the above stacktrace would result in the following:

```
java.lang.UnsupportedOperationException: This doesn't work

	at com.github.pcbouman_eur.testing.sanitze.tests.SanitizedTestClass$StudentMockClass.doSomethingElse(SanitizedTestClass.java:41)
	at com.github.pcbouman_eur.testing.sanitze.tests.SanitizedTestClass$StudentMockClass.doSomething(SanitizedTestClass.java:37)
	at TeacherTestClass.testCaseMethod(Native Method)
```

Note that any details of the platforms are hidden, and therefore the student will always see `TeacherTestClass.testCaseMethod` at the
point where the stack trace is cut-off. Using the option to sanitize stack traces can be done using the `@SanitizeExceptions` on a
test class, for example:

```java
import org.junit.jupiter.api.Test;
import com.github.pcbouman_eur.testing.sanitze.SanitizeExceptions;

@SanitizeExceptions
public class MyTest {
    @Test
    public void firstTestMethod() {
        assertEquals(8, 5+3, "The sum of five and three should be eight");
        assertEquals(1, 1*1, "The product of one and one should be one");
    }

    @Test
    public void firstTestMethod() {
        assertEquals(42, 6*9, "The product of six and nine should be forty-two");
    }
}
```

The annotation will make sure a JUnit plugin is loaded when the test are executed.
This plugin will intercept any exceptions that are thrown, rewrite the stack trace
on the exception object, and then let the exception continue to propagate.
When a test is started, the class
and method of the test are used to determine at which point the stack trace should 
be cut off.

**Note:** the strategy for sanitizing stack traces assumes that students do not use
any reflection. Since this is a very advanced topic, this should not be an issue as
students who can handle reflection, should probably also be able to handle non-sanitized
stack traces.


## Soft Assertions

The `soft-assert` module provides a way to turn standard assertions in soft assertions.
When a soft assertion fails, this information is collected but an `AssertionFailedError`
is not thrown immediately but when the test ends. This means that the test keeps running
and more detailed information about which assertions fail and success can be collected.

In order to make use of soft assertions, two steps are needed:

1. The assertions, such as `assertEquals(...)` and `assertThrows(...)` should be replaced by versions of these methods that allow SoftAssertions to occur.
2. A context for soft assertions must be activated in order to collect exceptions that occur while running a test case.

As a first step, it is necessary to replace the standard JUnit static assertion methods
by the static assertion methods in the `com.github.pcbouman_eur.testing.soft_assert.SoftAssertions`
class. This class is automatically generated by the `soft-assert-generate` module of the
project.

Usually, this is very easy. When working with JUnit5 it is common to writing a static import
such as the following:

```java
import static org.junit.jupiter.api.Assertions.*;
```

it is sufficient to replace this line with 

```java
import static com.github.pcbouman_eur.testing.soft_assert.SoftAssertions.*;
```

and all assertions in the class now make use of the new static methods.

However, by default the static methods in `SoftAssertions` still immediately throw a `AssertionFailedError`
unless they detect that a soft assertion context was *initialized*. Fortunatelly, this context can be
activate for testcases or test classes by adding the `@SoftAssert` annotation. Adding this annotation will
ensure that the proper JUnit plugin is loaded when these test cases are executed. The plugin itself will
listen for the start of the test to initialize the soft assertion context, and also listen for the end of
the test to throw an `AssertionFailedError` in case anything issues occured while running the test case.

### Full Example

Consider the following class that is implemented using standard JUnit features:

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MyTest {
    @Test
    public void firstTestMethod() {
        assertEquals(8, 5+3, "The sum of five and three should be eight");
        assertEquals(1, 1*1, "The product of one and one should be one");
    }

    @Test
    public void firstTestMethod() {
        assertEquals(42, 6*9, "The product of six and nine should be forty-two");
    }
}
```

Modifying it to use soft assertions gives us:

```java
import org.junit.jupiter.api.Test;
import com.github.pcbouman_eur.testing.soft_assert.SoftAssert;

import static com.github.pcbouman_eur.testing.soft_assert.SoftAssertions.*;

@SoftAssert
public class MyTest {
    @Test
    public void firstTestMethod() {
        assertEquals(8, 5+3, "The sum of five and three should be eight");
        assertEquals(1, 1*1, "The product of one and one should be one");
    }

    @Test
    public void firstTestMethod() {
        assertEquals(42, 6*9, "The product of six and nine should be forty-two");
    }
}
```

Note that it would be also possible to attach the `@SoftAssert` annotation only to
particular test methods, for example as follows:

```java
    @SoftAssert
    @Test
    public void firstTestMethod() {
        assertEquals(42, 6*9, "The product of six and nine should be forty-two");
    }
```

## Avoiding compiler errors in teacher code caused by student mistakes

The `core` module provides option to dynamically instantiate proxies to students
classes that dynamically call the methods of the classes. This way, no compiler
errors in teacher code will show up if the students forget to implement certain
methods or constructors. Instead, the proxy will thrown exception, such as
`MissingMethodException` or `MissingConstructorException` if certain things
are expected by the test code, but missing from the student code. 

In the future I will write more documentation here about how this can be used.

## Force students to use the default package

In my assignments I request that students make use of the *default* package, which
means that they should not use any package definition in their source files.
Sometimes it still happens that students do declare a package, after which their
code compiles successfully but their classes can't be found when running the tests.

The `compiler-plugin-package` modules provides a very simple Java Compiler Plugin
that throws and exception if it has to compile any classes outside the *default*
package.

Unfortunately, I am not using this plugin on Codegrade yet and more documentation
will be provided in the future.