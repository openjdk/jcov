# Synopsis

This repository contains source code for a tool which is capable of detecting **"simple methods"** in Java bytecode.

A **"simple method"**, with the exception of empty methods, is a method which contains some **"simple code"** followed by a specific set of Java bytecode instructions as explained below.

**"Simple code"**, then, is a code consisting of Java bytecode instructions which only bring values on stack and have no other side effect: loading field values, parameter values, constants, etc. You may see precise list of instructions in `openjdk.jcov.filter.simplemethods.Utils.SIMPLE_INSTRUCTIONS` field in the source.

Next types of "simple methods" are supported:
 * simple **getters**. A "simple code" followed by a return statement
 * simple **setters**. A "simple code" followed by setting a field
 * simple **delegators**. A "simple code" followed by a method call
 * simple **throwers**. A "simple code" followed by a throw statement
 * **empty methods**. Methods with empty bodies.

# Command line syntax
```
java -classpath jcov.jar:SimpleMethods.jar openjdk.jcov.filter.simplemethods.Scanner --usage

java -classpath jcov.jar:SimpleMethods.jar openjdk.jcov.filter.simplemethods.Scanner [--include|-i <include patern>] [--exclude|-e <exclude pattern>] \
[--getters <output file name>] [--setters <output file name>] [--delegators <output file name>] [--throwers <output file name>] [--empty <output file name>] \
jrt:/ | jar:file:/<jar file> | file:/<class hierarchy>

    Options
        --include - what classes to scan for simple methods.
        --exclude - what classes to exclude from scanning.
    Next options specify file names where to collect this or that type of methods. Only those which specified are detected. At least one kind of methods should be requested. Please consult the source code for exact details.
        --getters - methods which are just returning a value.
        --setters - methods which are just setting a field.
        --delegators - methods which are just calling another method.
        --throwers - methods which are just throwing an exception.
        --empty - methods with an empty body.

    Parameters define where to look for classes which are to be scanned.
        jrt:/ - scan JDK classes
        jar:file:/ - scan a jar file
        file:/ - scan a directory containing compiled classes.
```
# Building
```
ant jar
```

To build, a `jcov.jar` is required. The `jcov.jar` should contain ASM of version 6.0 or newer.

Build requires JDK 9.0 or newer.

# Testing

To run tests, TestNG is required. TestNG must be specified by `testng.classpath` property:
```
ant -Dtestng.classpath=<testng.jar>:<jcommander.jar> test
```

# Author
Alexander (Shura) Ilin (alexandre.iline@oracle.com)
