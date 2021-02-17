/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p> SE implementation of PropertyFinder </p>
 *
 * @author Dmitry Fazunenko
 * @author Alexey Fedorchenko
 */
public final class PropertyFinder {

    /**
     * <p> Reads input string substituting macros. No additional shortcarts
     * used. </p>
     *
     * @param str String to parse and substitute
     * @return Parsed string
     */
    public static String processMacroString(String str) {
        return processMacroString(str, null, null);
    }

    /**
     * <p> Reads input string substituting macros. Additional shortcarts can be
     * used to enhance or overwrite default macros. </p>
     *
     * @param str String to parse and substitute
     * @param shortcarts
     * @param datas
     * @return
     */
    public static String processMacroString(String str, char shortcarts[], String datas[]) {
        if (str == null) {
            return str;
        }
        StringBuilder buf = new StringBuilder();
        int start = 0, pos = 0;
        while (true) {
            pos = str.indexOf('%', start);
            if (pos < 0) {
                buf.append(str.substring(start));
                break;
            }
            buf.append(str.substring(start, pos));
            int end = str.indexOf('%', pos + 1);
            if (end < 0) {
                buf.append(str.substring(pos));
                break;
            }
            String patt = str.substring(pos, end);
            if (end - pos < 2) { // %
                buf.append('%');
            } else {
                char ch = patt.charAt(1);
                if (end - pos == 2) { // prebuilt patterns
                    boolean found = false;
                    if (shortcarts != null) {
                        for (int i = 0; i < shortcarts.length; ++i) {
                            if (shortcarts[i] == ch) {
                                found = true;
                                buf.append(datas[i]);
                            }
                        }
                    }
                    if (!found) {
                        switch (ch) {
                            case 'd': // M-D-Y
                                Calendar c = Calendar.getInstance();
                                buf.append(c.get(Calendar.HOUR_OF_DAY)).append(':').
                                        append(c.get(Calendar.MINUTE)).append(':').
                                        append(c.get(Calendar.SECOND)).append('_').
                                        append(c.get(Calendar.MONTH) + 1).append('-').
                                        append(c.get(Calendar.DAY_OF_MONTH)).append('-').
                                        append(c.get(Calendar.YEAR));
                                break;
                            case 't': // h:m:s
                                c = Calendar.getInstance();
                                buf.append(c.get(Calendar.HOUR_OF_DAY)).append(':').
                                        append(c.get(Calendar.MINUTE)).append(':').
                                        append(c.get(Calendar.SECOND));
                                break;
                            case 'D': // VM workdir
                                buf.append(System.getProperty("user.dir"));
                                break;
                            case 'R': // random int
                                buf.append(Math.round(Math.random() * 100000));
                                break;
                            case 'T': // time
                                buf.append(System.currentTimeMillis());
                                break;
                            case 'U': // username
                                buf.append(System.getProperty("user.name"));
                                break;
                            case 'V': // JAVA version
                                buf.append(System.getProperty("java.version"));
                                break;
                            default:
                                --end; // including last % to next search
                                buf.append(patt);
                                break;
                        }
                    }
                } else if (ch == 'F') { // field
                    String className = patt.substring(2, patt.lastIndexOf('.'));
                    try {
                        Class c = Class.forName(className);
                        Field f = c.getDeclaredField(patt.substring(patt.lastIndexOf('.') + 1, patt.length()));
                        boolean changed = false;
                        if (!f.isAccessible()) {
                            f.setAccessible(true);
                            changed = true;
                        }
                        try {
                            if (f != null) {
                                buf.append(f.get(null).toString());
                            } else {
                                --end; // including last % to next search
                                buf.append(patt);
                            }
                        } finally {
                            if (changed) {
                                f.setAccessible(false);
                            }
                        }
                    } catch (Exception e) {
                        --end; // including last % to next search
                        buf.append(patt);
                    }
                } else if (ch == 'M') { // method
                    String className = patt.substring(2, patt.lastIndexOf('.'));
                    try {
                        Class c = Class.forName(className);
                        Method m = c.getDeclaredMethod(patt.substring(patt.lastIndexOf('.') + 1, patt.length()), (Class[]) null);
                        boolean changed = false;
                        if (!m.isAccessible()) {
                            m.setAccessible(true);
                            changed = true;
                        }
                        try {
                            if (m != null && m.getReturnType() != Void.class) {
                                buf.append(m.invoke(null, (Object[]) null).toString());
                            } else {
                                --end; // including last % to next search
                                buf.append(patt);
                            }
                        } finally {
                            if (changed) {
                                m.setAccessible(false);
                            }
                        }
                    } catch (Exception e) {
                        --end; // including last % to next search
                        buf.append(patt);
                    }
                } else if (ch == 'E' || ch == 'P') { // environment variable or Java property
                    String prop = System.getenv(patt.substring(2));
                    if (prop != null) {
                        buf.append(prop);
                    } else {
                        --end; // including last % to next search
                        buf.append(patt);
                    }
                } else { // Java property
                    String prop = System.getProperty(patt.substring(1));
                    if (prop != null) {
                        buf.append(prop);
                    } else {
                        --end; // including last % to next search
                        buf.append(patt);
                    }
                }
            }
            start = end + 1;
        }
        return buf.toString();
    }
    private static Properties p;
    private static boolean propsRead = false;
    private static String propsFile;
    public static final String PROPERTY_FILE_PREFIX = "jcov.";
    public static final String JVM_PROPERTY_PREFIX = PROPERTY_FILE_PREFIX;
    public static final String ENV_PROPERTY_PREFIX = "JCOV_";

    /**
     * <p> Returns value specified by user. If sys prop defined the value is
     * taken from system property, if not the looks for env variable setting and
     * the default value is taken in the last turn. </p>
     *
     * @param name - variable name. JCOV_{NAME} is used for sys env, jcov.{name}
     * is used for jvm env
     * @param defaultValue - default value
     * @return
     */
    public static String getStaticValue(String name, String defaultValue) {
        try {
            String res = System.getProperty(PROPERTY_FILE_PREFIX + name);
            if (res != null) {
                return res;
            }
            res = System.getenv(ENV_PROPERTY_PREFIX + name.replace('.', '_').toUpperCase());
            if (res != null) {
                return res;
            }
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    /**
     * <p> Returns value specified by user. If sys prop is defined the value is
     * taken from system property, if not the looks for env variable setting, if
     * not it looks in property files and the default value is taken in the last
     * turn. </p>
     *
     * @param name - variable name. JCOV_{NAME} is used for sys env, jcov.{name}
     * is used for jvm env
     * @param defaultValue - default value
     * @return
     */
    public static String findValue(String name, String defaultValue) {
        String res = getStaticValue(name, null);

        if (res == null) {
            Properties p = findProperties();
            if (p != null) {
                res = p.getProperty(PROPERTY_FILE_PREFIX + name, defaultValue);
            } else {
                res = defaultValue;
            }
        }

        return processMacroString(res, null, null);
    }

    /**
     * <p> Searches for jcov property file. First candidate to read is file in
     * JCOV_PROPFILE system env variable. Second candidate is file in
     * jcov.propfile jvm env. Third candidate to read is
     * /com/sun/tdk/jcov/jcov.properties classpath resourse. Last candidate is
     * {user.home}/.jcov/jcov.properties file. </p> <p> Every filename is
     * firstly checked as a file and is read only if such file exists and can be
     * read. If it's not a file, can't be read, doesn't exist or is not a
     * property file then classpath resource is checked. </p>
     *
     * @return Properties read from all possible sources or null if not found.
     */
    private static Properties findProperties() {
        if (!propsRead) {
            propsRead = true;
            String propfile = getStaticValue("propfile", null); // jcov.propfile or JCOV_PROPFILE

            if (propfile != null) {
                p = readProperties(propfile);
                if (p != null) {
                    propsFile = propfile;
                    return p;
                }
            }

            if (propfile == null || !("/com/sun/tdk/jcov/jcov.properties".equals(propfile))) {
                p = readProperties("/com/sun/tdk/jcov/jcov.properties");
                if (p != null) {
                    propsFile = "/com/sun/tdk/jcov/jcov.properties";
                    return p;
                }
            }

            try {
                p = readProperties(System.getProperty("user.home") + File.separator + ".jcov" + File.separator + "jcov.properties");
                if (p != null) {
                    propsFile = System.getProperty("user.home") + File.separator + ".jcov" + File.separator + "jcov.properties";
                }
            } catch (Exception ignore) {
            }
        }

        return p;
    }

    private static Properties loadPropertiesFile(String path, Properties properties) {
        try(InputStream in = new FileInputStream(path)) {
            Properties p = ( properties == null) ?  new Properties() : properties;
            p.load(in);
            resolveProps(p);
            properties = p;
        } catch (Exception ignore) {
            // warning message
        }
        return properties;
    }

    private static Properties loadPropertiesStream(String path, Properties properties) {
        try(InputStream in = JCovSaver.class.getResourceAsStream(path)) {
            Properties p = ( properties == null) ?  new Properties() : properties;
            p.load(in);
            resolveProps(p);
            properties = p;
        } catch (Exception ignore) {
            // warning message
        }
        return properties;
    }

    /**
     * <p> Reads jcov property file from specified path </p> <p>
     * If it can't be read then classpath resource is checked. <p>
     *
     * @param path Path to look for a property file.
     * @return Read properties or null if file was not found neither in file
     * system neither in classpath
     */
    public static Properties readProperties(String path) {
        Properties p = loadPropertiesFile(path, null);
        return  p == null ? loadPropertiesStream(path, null) : p;
    }

    /**
     * <p> Reads jcov property file from specified path </p> <p>
     * If it can't be read then classpath resource is checked. <p>
     *
     * @param path Path to look for a property file.
     * @return Read properties or null if file was not found neither in file
     * system neither in classpath
     */
    public static Properties readProperties(String path, Properties properties) {
        if (properties == null) {
            properties = new Properties();
        }
        Properties p = loadPropertiesFile(path, properties);
        return  properties.equals(p) ? loadPropertiesStream(path, properties) : p;
    }

    /**
     * <p> Resolves all links of ${key} form on other keys in property values.
     * </p>
     *
     * @param props Properties to resolve.
     */
    private static void resolveProps(Properties props) {
        Pattern p = Pattern.compile(".*(\\$\\{(.*)\\})");
        for (Object o : props.keySet()) {
            String name = (String) o;
            String val = props.getProperty(name);
            Matcher m = p.matcher(val);
            while (m.find()) {
                String link = m.group(2);
                String lVal = props.getProperty(link);
                val = val.replace(m.group(1), lVal);
                m = p.matcher(val);
            }
            props.put(o, val);
        }
    }

    /**
     * <p> Read a single property from property file </p>
     *
     * @param fileName file to look value in
     * @param name name of value to read
     * @return value of "name" in fileName property file or null if such
     * property file doesn't exist
     */
    public static String readPropFrom(String fileName, String name) {
        Properties props = readProperties(fileName);
        if (props != null) {
            return props.getProperty(PROPERTY_FILE_PREFIX + name);
        } else {
            return null;
        }
    }

    /**
     * <p> Describes source of a property by name. Returns a string containing
     * description of the property source. E.g.: </p> <ul> <li> "JavaVM property
     * 'jcov.propfile' </li> <li> "system environment property 'JCOV_TEMPLATE'
     * </li> <li> "property file from '/temp/jcov/jcov.properties' </li> <li>
     * "defaults" </li> </ul>
     *
     * @param name Property name to check source
     * @return String describing property source.
     */
    public static String findSource(String name) {
        if (name == null || name.isEmpty()) {
            return "";
        }

        if (System.getProperty(JVM_PROPERTY_PREFIX + name) != null) {
            return "JavaVM property '" + JVM_PROPERTY_PREFIX + name + "'";
        }

        if (System.getenv(ENV_PROPERTY_PREFIX + name.toUpperCase()) != null) {
            return "system environment property '" + ENV_PROPERTY_PREFIX + name.toUpperCase() + "'";
        }

        if (!propsRead) {
            findProperties();
        }

        if (propsFile != null && p.containsKey(PROPERTY_FILE_PREFIX + name)) {
            return "property file from '" + propsFile + "'";
        }

        return "defaults";
    }

    /**
     * <p> Set path for properties file to read values. Can be used many times.
     * </p>
     *
     * @param path Path to read
     */
    public static void setPropertiesFile(String path) {
        propsFile = path;
        p = readProperties(path);
        propsRead = true;
    }

    public static void cleanProperties() {
        p = null;
        propsFile = null;
        propsRead = false;
    }

    /**
     * <p> Installs shutdown hook. ME/Card version can't install shutdown hook.
     * </p>
     */
    public static void addAutoShutdownSave() {
        if (Collect.saveAtShutdownEnabled && "true".equals(findValue("autosave", "true"))) {
            try {
                Runtime.getRuntime().addShutdownHook(new Thread() {
                    @Override
                    public void run() {
                        Collect.disable();
                        Collect.saveResults();
                        //TODO is this the right place and the right way to handle the saver?
                        String s = PropertyFinder.findValue("data-saver", null);
                        if(s != null) {
                            try {
                                Class clz = Class.forName(s);
                                Object saver = clz.getConstructor().newInstance();
                                Method mthd = clz.getMethod("saveResults", new Class[] {});
                                mthd.invoke(saver);
                            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | 
                                     InvocationTargetException | InstantiationException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        Collect.enable();
                        Collect.saveAtShutdownEnabled = false;
                        Collect.saveEnabled = false;
                    }
                });
            } catch (Exception ignore) {
                System.err.println("Can't set shutdown hook.");
            }
        }
    }

    /**
     * <p> Checks whether VM is ready to initialize JCov RT (saver). Most savers
     * use shutdown hook to save data in time. Shutdown hook needs Thread to be
     * created but it can't be created in very early VM livetime. </p> <p> Due
     * to restrictions JCovME version should have it's own isVMReady()
     * implementation. </p>
     *
     * @return true if VM is ready to install shutdown hook and to read
     * properties
     */
    public static boolean isVMReady() {
        return System.out != null && Runtime.getRuntime() != null;//&& sun.misc.VM.isBooted();
    }

}
