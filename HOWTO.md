# Usage Guide of Student Test Tools

This brief document gives some points on how you can use these student test tools on CodeGrade's Autotest V2.

## Installation

To install, add the following script to the *Setup* part of your AutoTest configuration.

```bash
EXPORT version=0.9.0

wget --quiet "https://github.com/pcbouman-eur/student-test-tools/releases/download/v$VERSION/testing-lib-$VERSION.jar"

sudo java -jar "testing-lib-$VERSION.jar" install

rm "testing-lib-$VERSION.jar"

```

Sometimes, you may want to specify additional maven artifacts that should be available on the classpath.

This can be done as follows, where the pattern `<groupId>:<artifactId>:<version>` is used to specify packages living on Maven central.


```bash
EXPORT version=0.9.0

wget --quiet "https://github.com/pcbouman-eur/student-test-tools/releases/download/v$VERSION/testing-lib-$VERSION.jar"


sudo java -jar "testing-lib-$VERSION.jar" install \
 -a org.apache.commons:commons-math3:3.6.1 \
 -a org.apache.poi:poi:4.1.2 \
 -a org.apache.poi:poi-ooxml:4.1.2 \

rm "testing-lib-$VERSION.jar"
```

## Compilation

In the student setup script, the most basic way to handle compilation is to use

```bash
sttest-compile
```

Sometimes, you may want to specify that some teacher provided sources live in the fixtures. This can be done as follows:

```bash
sttest-compile -p $FIXTURES/TeacherClass1.java -p $FIXTURES/TeacherClass2.java -p $FIXTURES/TeacherClass3.java

```

By default, the command assumes the student source files are all `.java` files that live in the current working directory,
while the teacher source files are all `.java` files living in `$FIXTURES`, except the ones that are specifilly indicated
to be *provided* source files.



The advantage of this compilation approach is that it runs in two phases to separate compilation errors in student code

from potential compilation errors in teacher code caused by student mistakes.



In general, source files are treated as one of three types by this procedure:



1. *Provided source code*: These are files the teacher provides to students and which students should not change.

2. *Student source code*: These are files the student uploads as their solution to an assignment.

3. *Teacher source code*: This is typically source code used by the teacher to test the student code.

The compilation occurs in two phases, that work as follows:

**Phase 1:** The provided source code and student source code is compiled together. If any compilation errors occur,
these are presented to the student as there mistake, since it occurs in code they can also work with on
their own device. Only if this step occurs without errors, the second phase is used.

**Phase 2:** The provided source code, student source code and teacher source code are compiled together. Any compilation
errors now must occur in the teacher source code, which would typically indicate the student made a mistake
in implementing the specification of the assignment correctly. Hence, the output of the compiler is enhanced
with a disclaimer that the student should carefully check if their code adheres to the specification.

## Check if Compilation Succeeded

In case you want to have a step in the testing procedure that awards points when classes have compiled succesfully,
you can use

```bash
sttest-compile-check StudentClass1 StudentClass2 StudentClass3
```

If there were compilation errors, these are shown again as the output of this step. Otherwise, this step will complete succesfully.

## Running Code Quality and Style Checks

To run checkstyle together with the Pmd-from-Checkstyle plugin, you can use the following command:

```bash
sttest-checkstyle
```

By default, this will check if the file `$FIXTURES/checkstyle.xml` exists an use that. If no file exists, it will use the default checkstyle configuration.

Alternatively, you can specify a custom config file to use with the `--config` argument. For example:

```bash
sttest-checkstyle --config $FIXTURES/checkstyle-custom.xml
```

The advantage of this command over the regular 

## Using the Customized JUnit Framework

While some features of the JUnit plugins can be easily used with the standard Codegrade Autotest V2 JUnit blocks, such as the `@SanitizeErrors` plugin,
the soft assertion module produces currently very cumbersome errors and reports that way. To use these specific features, instead you can use the following
command to run JUnit Tests:

```bash
sttest-run -c TeacherTestClass1 -c TeacherTestClass2 -t tag1 -t tag2
```

One or more test classes and one or more tags can be specified. If no tags are specified, all tests from the testclasses will be run. If there are tags specified,
only the tests with the specified tags are being run.

