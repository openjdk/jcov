/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.jcov.data;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Much of the functionality in this plugin is controlled through system properties. This class defines some shortcuts
 * which makes it a bit easier.
 */
public class Env {

    public static void properties(Map<String, String> properties) {
        properties.forEach((k, v) -> System.setProperty(k, v));
    }
    public static void clear(String prefix) {
        Set<String> keys = System.getProperties().stringPropertyNames();
        keys.stream().filter(k -> k.toString().startsWith(prefix))
                .forEach(k -> System.clearProperty(k));
    }

    public static String getStringEnv(String property, String defaultValue) {
        return System.getProperty(property, defaultValue);
    }
    public static Path getPathEnv(String property, Path defaultValue) {
        String propValue = System.getProperty(property);
        if(propValue != null) return Path.of(propValue);
        else return defaultValue;
    }
    public static <SPI> SPI getSPIEnv(String property, SPI defaultValue) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        String propValue = System.getProperty(property);
        if(propValue != null) {
            if (!propValue.contains("("))
                return (SPI) Class.forName(propValue).getConstructor().newInstance();
            else {
                int ob = propValue.indexOf('(');
                int cb = propValue.indexOf(')');
                Class cls = Class.forName(propValue.substring(0, ob));
                String[] params = propValue.substring(ob, cb - 1).split(",");
                Class[] paramTypes = new Class[params.length];
                Arrays.fill(paramTypes, String.class);
                return (SPI) cls.getConstructor(paramTypes).newInstance(params);
            }
        } else return defaultValue;
    }
}
