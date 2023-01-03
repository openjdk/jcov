import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import com.sun.tdk.jcov.instrument.Modifiers;
import com.sun.tdk.jcov.instrument.asm.ASMInstrumentationPlugin;
import com.sun.tdk.jcov.instrument.asm.ASMModifiers;

module jcov.asm {
    exports com.sun.tdk.jcov.instrument.asm;
    requires jcov;
    requires org.objectweb.asm;
    requires org.objectweb.asm.util;
    requires org.objectweb.asm.tree;
    requires java.logging;
    provides InstrumentationPlugin with ASMInstrumentationPlugin;
    provides Modifiers.ModifiersFactory with ASMModifiers.ASMModfiersFactory;
}