package com.sun.tdk.jcov.instrument;

import java.nio.file.Path;

public interface InstrumentationPlugin {
    /**
     * Called after all instrumentation is complete.
     *
     * @throws Exception should some
     */
    void instrumentationComplete() throws Exception;

    /**
     * For the instrumented code to work independently (i.e. without adding additional classes  to the classpath), some
     * classes can be "implanted" into the instrumented code.
     *
     * @return Path containing the classes to be implanted. Must be in a form which can be added to Java classpath.
     */
    //TODO perhaps this can return a list of classes to be implanted
    Path runtime() throws Exception;

    /**
     * Name of a package which contains code, that will be called from the instrumented
     * code. Such package may need to be exported from a module.
     *
     * @return package name
     */
    String collectorPackage();
}
