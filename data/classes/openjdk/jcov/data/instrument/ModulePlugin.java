/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package openjdk.jcov.data.instrument;

import com.sun.tdk.jcov.instrument.ModuleInstrumentationPlugin;
import jdk.internal.classfile.Attributes;
import jdk.internal.classfile.ClassModel;
import jdk.internal.classfile.Classfile;
import jdk.internal.classfile.attribute.ModuleAttribute;
import jdk.internal.classfile.attribute.ModuleExportInfo;
import jdk.internal.classfile.attribute.ModuleHashesAttribute;
import jdk.internal.classfile.attribute.ModulePackagesAttribute;
import jdk.internal.classfile.java.lang.constant.PackageDesc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModulePlugin implements ModuleInstrumentationPlugin {
    @Override
    public String getModuleName(byte[] bytes) {
        ClassModel cls = Classfile.parse(bytes);
        if (cls.isModuleInfo()) {
            ModuleAttribute attr = ((ModuleAttribute) cls.attributes().stream()
                    .filter(a -> a.attributeMapper() == Attributes.MODULE)
                    .findFirst()
                    .orElseThrow());
            return attr.moduleName().name().stringValue();
        } else throw new IllegalStateException("Not a module!");
    }

    @Override
    public byte[] addExports(List<String> packages, byte[] bytes, ClassLoader loader) {
        ClassModel cls = Classfile.parse(bytes);
        if (cls.isModuleInfo()) {
            return cls.transform((builder, element) -> {
                if(element instanceof ModuleAttribute) {
                    ModuleAttribute me = (ModuleAttribute) element;
                    List<ModuleExportInfo> newExports = new ArrayList<>(me.exports());
                    newExports.addAll(packages.stream().map(p ->
                            ModuleExportInfo.of(PackageDesc.of(p), 0)).collect(Collectors.toList()));
                    builder.with(ModuleAttribute.of(me.moduleName(), me.moduleFlagsMask(), me.moduleVersion().get(),
                            me.requires(), newExports, me.opens(), me.uses(), me.provides()));
                } else if (element instanceof ModulePackagesAttribute) {
                    ModulePackagesAttribute mpe = (ModulePackagesAttribute) element;
                    List<PackageDesc> newPackages = new ArrayList<>(mpe.packages().stream()
                            .map(pe -> PackageDesc.of(pe.name().stringValue().replace('/', '.')))
                            .collect(Collectors.toList()));
                    newPackages.addAll(packages.stream().map(PackageDesc::of).collect(Collectors.toList()));
                    builder.with(ModulePackagesAttribute.ofNames(newPackages));
                } else builder.with(element);
            });
        } else throw new IllegalStateException("Not a module!");
    }

    @Override
    public byte[] clearHashes(byte[] bytes, ClassLoader loader) {
        ClassModel cls = Classfile.parse(bytes);
        if (cls.isModuleInfo()) {
            return cls.transform((builder, element) -> {
                if(element instanceof ModuleHashesAttribute) {
                } else builder.with(element);
            });
        } else throw new IllegalStateException("Not a module!");
    }
//
//    public static void main(String[] args) throws IOException {
//        Path module_info = FileSystems.getFileSystem(URI.create("jrt:/")).getRootDirectories().iterator().next()
//                .resolve("/modules").resolve("java.base").resolve("module-info.class");
//        byte[] orig = Files.readAllBytes(module_info);
//        byte[] addd = new ModulePlugin().addExports(List.of("other.pkg"), orig, ClassLoader.getSystemClassLoader());
//        addd = new ModulePlugin().clearHashes(addd, ClassLoader.getSystemClassLoader());
//        System.out.println(orig.length);
//        System.out.println(addd.length);
////        System.out.println(module_info);
//    }
}
