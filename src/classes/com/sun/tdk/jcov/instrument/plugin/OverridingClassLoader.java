/*
 * Copyright (c) 2023 Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.instrument.plugin;

import com.sun.tdk.jcov.instrument.InstrumentationPlugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;

/**
 * A classloader to first load classes prom the provided location.
 */
public class OverridingClassLoader extends URLClassLoader {

    private static URL[] toURL(Path root) {
        try {
            return new URL[]{root.toUri().toURL()};
        } catch (MalformedURLException e) {
            //should not happen since getting teh URL legally
            throw new RuntimeException(e);
        }
    }

    private final ClassLoader backup;

    public OverridingClassLoader(Path root, ClassLoader backup) {
        this(toURL(root), backup);
    }

    public OverridingClassLoader(URL[] urls, ClassLoader backup) {
        super(urls);
        this.backup = backup;
    }

    @Override
    public URL getResource(String name) {
        //first try to find local resource, from the current module
        URL resource = findResource(name);
        //for module-info it does not make sense to look in other classloaders
        if (name.equals(InstrumentationPlugin.MODULE_INFO_CLASS)) return resource;
        //if none, try other modules
        if (resource == null) resource = backup.getResource(name);
        //that should not happen during normal use
        //if happens, refer to super, nothing else we can do
        if (resource == null) resource = super.getResource(name);
        return resource;
    }
}
