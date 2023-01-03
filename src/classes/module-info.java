import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.Modifiers;

module jcov {
    exports com.sun.tdk.jcov.instrument;
    exports com.sun.tdk.jcov.io;
    exports com.sun.tdk.jcov.util;
    exports com.sun.tdk.jcov.data;
    exports com.sun.tdk.jcov.runtime;
    exports com.sun.tdk.jcov;
    exports com.sun.tdk.jcov.report;
    exports com.sun.tdk.jcov.report.ancfilters;
    exports com.sun.tdk.jcov.processing;
    requires java.logging;
    requires ant;
    requires java.xml;
    requires jdk.compiler;
    requires javatest;
    requires jdk.jdeps;
    uses InstrumentationPlugin;
    uses Modifiers.ModifiersFactory;
}