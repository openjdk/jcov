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
package openjdk.jcov.data;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Much of the functionality in this plugin is controlled through system properties. This class defines some shortcuts
 * which makes it a bit easier.
 */
public class Env {

    public static final String PROP_FILE =
            Env.class.getPackageName().replace('.', '/') +
            "/coverage.properties";
    /**
     * Prefix for all system property names which will be passed to the VM running JCov calls.
     */
    public static final String JCOV_DATA_ENV_PREFIX = "jcov.data.";

    private static final Properties DEFAULT_STRINGS = new Properties();
    private static final Map<String, Object> DEFAULT_SPI = new HashMap<>();

    public static void setSystemProperties(Map<String, String> properties) {
        properties.forEach((k, v) -> System.setProperty(k, v));
    }

    public static void clear(String prefix) {
        Set<String> keys = System.getProperties().stringPropertyNames();
        keys.stream().filter(k -> k.startsWith(prefix))
                .forEach(k -> System.clearProperty(k));
    }

    public static String getStringEnv(String property, String defaultValue) {
        synchronized(Env.class) {
                try {
                    InputStream in = Env.class.getResourceAsStream("/" + PROP_FILE);
                    if(in != null) {
                        DEFAULT_STRINGS.load(in);
                        System.out.println("Using property definitions from " + PROP_FILE + ":");
                        DEFAULT_STRINGS.list(System.out);
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (Throwable e) {
                    e.printStackTrace();
                    throw e;
                }
//            }
        }
        String override = System.getProperty(property);
        return (override != null) ? override : DEFAULT_STRINGS.getProperty(property, defaultValue);
    }

    public static Path getPathEnv(String property, Path defaultValue) {
        String propValue = getStringEnv(property, null);
        if(propValue != null) return Path.of(propValue);
        else return defaultValue;
    }

    public static <SPI> SPI getSPIEnv(String property, SPI defaultValue) throws ClassNotFoundException,
            NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        if(DEFAULT_SPI.containsKey(property)) {
            return (SPI)DEFAULT_SPI.get(property);
        }
        String propValue = getStringEnv(property, null);
        if(propValue != null) {
            if (!propValue.contains("("))
                return (SPI) Class.forName(propValue).getConstructor().newInstance();
            else {
                int ob = propValue.indexOf('(');
                int cb = propValue.indexOf(')');
                Class cls = Class.forName(propValue.substring(0, ob));
                String[] params = propValue.substring(ob + 1, cb).split(",");
                Class[] paramTypes = new Class[params.length];
                Arrays.fill(paramTypes, String.class);
                return (SPI) cls.getConstructor((Class<?>[])paramTypes).newInstance((Object)params);
            }
        } else return defaultValue;
    }
}
