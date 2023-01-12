package com.sun.tdk.jcov.instrument;

import java.util.List;

public interface ModuleInstrumentationPlugin {
    String getModuleName(byte[] moduleInfo);

    byte[] addExports(List<String> exports, byte[] moduleInfo, ClassLoader loader);

    byte[] clearHashes(byte[] moduleInfo, ClassLoader loader);
}
