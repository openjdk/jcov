package openjdk.jcov.data.arguments.jreinstr.filepermission;

import com.sun.tdk.jcov.instrument.InstrumentationPlugin;
import org.objectweb.asm.MethodVisitor;

import java.nio.file.Path;

public class VoidPlugin implements InstrumentationPlugin {

    public VoidPlugin() {
        super();
    }

    @Override
    public MethodVisitor methodVisitor(int i, String s, String s1, String s2, MethodVisitor visitor) {
        return visitor;
    }

    @Override
    public void instrumentationComplete() throws Exception {

    }

    @Override
    public Path runtime() throws Exception {
        return null;
    }

    @Override
    public String collectorPackage() {
        return null;
    }
}
