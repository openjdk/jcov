# JCov data plugin

This is an implementation of *com.sun.tdk.jcov.instrument.InstrumentationPlugin* plugin which can help with investigating of data flow within Java code. The plugin is intended to add additional instrumentation to collect facts about values used in runtime.

Currently this repository only contains code allowing to capture information about method arguments.

Code from *com.sun.tdk.jcov.instrument.plugin* JCov unit tests will later be also migrated  into this project to be able to collect data about values assigned to fields.

WIP!

Please see tests to find out how this can be used.
