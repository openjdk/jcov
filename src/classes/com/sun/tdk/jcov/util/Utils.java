/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.util;

import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException;
import com.sun.tdk.jcov.tools.LoggingFormatter;
import org.objectweb.asm.Opcodes;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.UnknownHostException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Formatter;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * This class implements miscellaneous utilities, necessary for Jcov
 */
public final class Utils {

    /**
     * The ASM API version that should be used by jcov.
     */
    public static final int ASM_API_VERSION = Opcodes.ASM9;

    private static Handler loggerHandler = null;
    private final static int ASCII_CHARS_TOTAL = 128;
    private static char[] buf = new char[32];
    private static File[] fileSysRoots;
    private static boolean fileSysRootsGot = false;
    /**
     * Represent digits in a big-radix numeric system
     */
    final static char[] chars = {
        'q', 'w', 'r', 't', 'y', 'u', 'i', 'o', 'p', 's',
        'g', 'h', 'j', 'k', 'l', 'z', 'x', 'v', 'n', 'm',
        'Q', 'W', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 'S',
        'G', 'H', 'J', 'K', 'L', 'Z', 'X', 'V', 'N', 'M',
        '!', '@', '#', '$', '%', '^', '&', '*', '(', ')',
        '_', '+', '|', '~', '-', '=', '\\', '[', ']', '{',
        '}', ';', '\'', ':', '\"', ',', '.', '/', '<', '>', '?'
    };
    public final static int radix = chars.length;
    static int[] map = new int[ASCII_CHARS_TOTAL];
    private static final Logger logger = Logger.getLogger("com.sun.tdk.jcov");

    static {
        for (int i = 0; i < radix; i++) {
            map[chars[i]] = i;
        }
    }

    public enum FILE_TYPE {
        ZIP, JAR, WAR, CLASS;
        public String getExtension() {
            return "." + this.name().toLowerCase();
        }

        public static boolean hasExtension(String fileName, FILE_TYPE... fTypes) {
           for(FILE_TYPE ftype : fTypes) {
               if( fileName.endsWith(ftype.getExtension()) ) {
                   return true;
               }
           }
           return false;
        }
    }

    /**
     * It's possible to set custom classfile extension by "clext" property (through jcov.clext system property, JCOV_CLEXT
     * environment variable and so on) - for example "clazz:.klass".
     */
    public static final List<String>  CUSTOM_CLASS_FILE_EXTENSIONS =
            Arrays.asList(PropertyFinder.findValue("clext", "").split(":").clone());

    public static boolean isClassFile(String fileName) {
        if(FILE_TYPE.hasExtension(fileName, FILE_TYPE.CLASS)) {
            return true;
        }
        return CUSTOM_CLASS_FILE_EXTENSIONS.stream().anyMatch(ext->fileName.endsWith(ext));
    }

    public final static int VER16  = 160;
    public final static int VER17  = 160;
    public final static int VER18  = 160;
    public final static int VER90  = 900;
    public final static int VER100 = 1000;
    private static int javaVersion = -1;

    /**
     * @return JVM version: java.version * 100
     */
    public static int getJavaVersion() {
        if (javaVersion == -1){
            String ver = System.getProperty("java.version");
            for(int i=1; i<=20; i++) {
                if( ver.startsWith(String.format( (i <= 8) ? "1.%d" : "%d" , i))) {
                    return (i <= 8) ? 100 + i*10 : i * 100;
                }
            }
            javaVersion = VER90;
        }
        return javaVersion;
    }

    /**
     * Checks if given array's length is not less than given length.
     *
     * @param buf array to be checked
     * @param length minimum required length
     * @param copy_old_buf whether to copy the contents of given buf to newly
     * created array, if minimum length requirement is not met
     * @return buf if the buf length is ok, newly created array of length +
     * length/2 size otherwise
     */
    public static byte[] ensureBufLength(byte[] buf, int length, boolean copy_old_buf) {
        if (buf.length >= length) {
            return buf;
        }
        byte[] tmp_buf = new byte[length + length / 2];
        if (copy_old_buf) {
            System.arraycopy(buf, 0, tmp_buf, 0, buf.length);
        }
        return tmp_buf;
    }

    /**
     * Constructs 'relative' path for given two paths. For example, for
     * "/aaa/bbb/ccc/ddd" and "/aaa/xxx/yyy" relative path would be
     * "../../../xxx/yyy". NOTE: this code is probably system dependent
     *
     * @param ref_path reference path
     * @param tst_path path to construct relative form for
     * @return constructed relative path
     */
    public static String getRelativePath(String ref_path, String tst_path) {
        String[] ref_arr = split(ref_path, File.separatorChar);
        String[] tst_arr = split(tst_path, File.separatorChar);
        int last_match_ind = -1;
        String res = "";

        for (int i = 0; i < ref_arr.length && i < tst_arr.length; i++) {
            if (!ref_arr[i].equals(tst_arr[i])) {
                break;
            }
            last_match_ind = i;
        }
        for (int i = 0; i < ref_arr.length - last_match_ind - 1; i++) {
            res += ".." + File.separator;
        }
        for (int i = last_match_ind + 1; i < tst_arr.length; i++) {
            res += tst_arr[i] + File.separator;
        }
        if (res.equals("")) {
            res = "." + File.separator;
        }
        return res.substring(0, res.length() - 1);
    }

    private static File[] getFileSystemRoots() {
        if (!fileSysRootsGot) {
            fileSysRoots = File.listRoots();
            fileSysRootsGot = true;
        }
        return fileSysRoots;
    }

    public static String getRelativePath(File ref_file, File tst_file) {
        File[] roots = getFileSystemRoots();
        String ref_path = null;
        String tst_path = null;

        try {
            ref_path = ref_file.getCanonicalPath();
            tst_path = tst_file.getCanonicalPath();
        } catch (IOException e) {
            ref_path = ref_file.getAbsolutePath();
            tst_path = tst_file.getAbsolutePath();
        }
        if (roots != null) {
            for (int i = 0; i < roots.length; i++) {
                String root = roots[i].getAbsolutePath();
                if (ref_path.startsWith(root) != tst_path.startsWith(root)) {
                    return null;
                }
            }
        }
        return getRelativePath(ref_path, tst_path);
    }

    public static String basename(String path) {
        int i1 = path.lastIndexOf(File.separatorChar);
        int i2 = path.lastIndexOf("/");
        int i = i1 > i2 ? i1 : i2;
        if (i == -1) {
            return path;
        }
        return path.substring(i + 1);
    }

    public static String substitute(String s, String from, String to) {
        if (s == null || from == null || s.equals("") || from.equals("")) {
            return s;
        }
        String res = "";
        int ind = s.indexOf(from);
        while (ind >= 0) {
            res += s.substring(0, ind);
            res += to;
            s = s.substring(ind + 1);
            ind = s.indexOf(from);
        }
        res += s;
        return res;
    }

    public static String[] split(String s, char delim) {
        String str = s;
        if (str == null) {
            return null;
        }
        Vector v = new Vector();
        for (int ind = str.indexOf(delim); ind >= 0; ind = str.indexOf(delim)) {
            if (ind > 0) {
                v.addElement(str.substring(0, ind));
            }
            str = str.substring(ind + 1);
        }
        if (str.length() > 0) {
            v.addElement(str);
        }
        String[] res = new String[v.size()];
        v.copyInto(res);
        return res;
    }

    public static String[] getSourcePaths(String s) {
        String[] source_paths = split(s, File.pathSeparatorChar);
        for (int i = 0; i < source_paths.length; i++) {
            String path = source_paths[i];
            if (!path.equals("") && !(path.endsWith("/") && !path.endsWith(File.separator))) {
                path += File.separator;
                source_paths[i] = path;
            }
        }
        return source_paths;
    }

    /**
     * Calculates string representation of given value in a numeric system,
     * where digits are all characters from the chars array field, and the radix
     * is chars.length, and writes it to given String buffer starting from ind.
     *
     * @param val value to convert
     * @param sbuf buffer where the representation of the value is written
     * @param ind starting index in sbuf
     * @return ind + length of the string representation
     * @see #chars
     */
    public static int convert2BigRadix(int val, StringBuffer sbuf, int ind) {
        int i = 0;
        while (val > 0) {
            buf[i++] = chars[val % radix];
            val = val / radix;
        }
        for (int j = 1; j <= i; j++) {
            sbuf.setCharAt(ind + j - 1, buf[i - j]);
        }
        return ind + i;
    }

    /**
     * @return value of the big-radix numeric system digit
     */
    public static int convert2Int(char digit) {
        return map[digit];
    }

    /**
     * @return value of the hex digit
     */
    public static byte hexChar2Int(char ch) {
        boolean f1 = ch >= '0' && ch <= '9';
        boolean f2 = ch >= 'A' && ch <= 'F';
        boolean f3 = ch >= 'a' && ch <= 'f';
        if (!(f1 || f2 || f3)) {
            return -1;
        }
        if (f1) {
            return (byte) (ch - '0');
        } else if (f2) {
            return (byte) (10 + ch - 'A');
        } else {
            return (byte) (10 + ch - 'a');
        }
    }

    /**
     * @return hexadecimal digit with given value
     */
    public static char int2HexChar(int val) {
        if (val >= 0 && val < 10) {
            return (char) (val + '0');
        }
        if (val >= 10 && val < 16) {
            return (char) (val + 'a' - 10);
        }
        return (char) -1;
    }

    /**
     * @return &lt;base&gt; in power of &lt;power&gt;
     */
    public static int pow(int base, int power) {
        int i, res = 1;
        for (i = 0; i < power; i++) {
            res *= base;
        }
        return res;
    }

    /**
     * Writes 4 lower bits of the &lt;half_byte&gt; to &lt;dst&gt; at
     * &lt;ind&gt;. ind = 1 corresponds to the 4 high bits in dst[0].
     */
    public static void writeHalfByteAt(byte half_byte, int ind, byte[] dst) {
        int i = ind / 2;
        byte val;
        if (ind % 2 == 0) {
            val = (byte) (dst[i] & 0xF0);
            dst[i] = (byte) (val | half_byte);
        } else {
            val = (byte) (dst[i] & 0xF);
            dst[i] = (byte) (val | (half_byte << 4));
        }
    }

    /**
     * @return 4-bit value from &lt;src&gt;{&lt;ind&gt;} (&lt;src&gt; treated as
     * an array of 4-bit values)
     */
    public static byte getHalfByteAt(int ind, byte[] src) {
        return (byte) ((ind % 2 == 0) ? src[ind / 2] & 0xF : (src[ind / 2] >>> 4) & 0xF);
    }

    /**
     * @return 4-bit value from &lt;src&gt; at &lt;ind&gt;
     */
    public static byte getHalfByteAt(int ind, byte src) {
        return (byte) ((ind % 2 == 0) ? src & 0xF : (src >>> 4) & 0xF);
    }

    /**
     * @return number of bit quadruples necessary to accomodate
     * &lt;bits_total&gt; bits
     */
    public static int halfBytesRequiredFor(int bits_total) {
        return (bits_total + 3) / 4;
    }

    /**
     * Compares 2 String arrays
     *
     * @return -1 if arr1 < arr2 0 if arr1 == arr2 1 if arr1 > arr2
     */
    public static int compareStringArrays(String[] arr1, String[] arr2) {
        boolean b1 = arr1 == null || arr1.length == 0;
        boolean b2 = arr2 == null || arr2.length == 0;
        if (b1 == true && b2 == true) {
            return 0;
        }
        if (b1 == true && b2 == false) {
            return -1;
        }
        if (b1 == false && b2 == true) {
            return 1;
        }
        int min_len = arr1.length > arr2.length ? arr2.length : arr1.length;
        for (int i = 0; i < min_len; i++) {
            int res = arr1[i].compareTo(arr2[i]);
            if (res != 0) {
                return res;
            }
        }
        return arr1.length > arr2.length ? 1 : -1;
    }

    public static <T> T[] copyOf(T[] original, int newLength) {
        return (T[]) copyOf(original, newLength, original.getClass());
    }

    public static <T, U> T[] copyOf(U[] original, int newLength, Class<? extends T[]> newType) {
        T[] copy = ((Object) newType == (Object) Object[].class)
                ? (T[]) new Object[newLength]
                : (T[]) Array.newInstance(newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                Math.min(original.length, newLength));
        return copy;
    }

    public static int[] copyOf(int[] original, int newLength) {
        int[] copy = new int[newLength];
        System.arraycopy(original, 0, copy, 0,
                Math.min(original.length, newLength));
        return copy;
    }

    /**
     * The same as
     * <code>readLines(fileName, -1, -1)</code>
     */
    public static String[] readLines(String fileName) throws IOException {
        return readLines(fileName, -1, -1);
    }

    /**
     * Reads the text files ignoring leading and tailing line spaces, and empty
     * lines.
     *
     * @param fileName file to read
     * @param start number of line to start from. -1 means start from 0.
     * @param end number of line to end with. -1 means read all lines till the
     * end.
     * @return read lines. Never returns null.
     * @throws java.io.IOException - if file does not exist, or i/o errors
     * happened
     */
    public static String[] readLines(String fileName, int start, int end) throws IOException {
        ArrayList lst = new ArrayList();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), Charset.defaultCharset()));
        String line;
        int i = 0;
        while ((line = reader.readLine()) != null) {
            if ((start == -1 || i >= start)
                    && (end == -1 || i <= end)) {//to check range
                line = line.trim();
                if (line.length() > 0 /*&& !line.startsWith("#")*/) {
                    lst.add(line);
                }
            }
            i++;
        }
        reader.close();
        return (String[]) lst.toArray(new String[0]);
    }

    /**
     * Writes the lines into the file
     *
     * @param fileName file to read
     * @throws java.io.IOException - if i/o errors happened
     */
    public static void writeLines(String fileName, String[] lines) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                fileName), Charset.defaultCharset()));
        for (int i = 0; i < lines.length; i++) {
            writer.write(lines[i]);
            writer.newLine();
        }
        writer.flush();
        writer.close();
    }

    public static void copyFile(File from, File to) throws FileNotFoundException, IOException {

        FileChannel infc = null;
        FileChannel outfc = null;
        try {
            infc = new FileInputStream(from).getChannel();
            outfc = new FileOutputStream(to).getChannel();
            long transfered = infc.transferTo(0, from.length(), outfc);
            while (transfered < from.length()) {
                transfered = infc.transferTo(transfered, from.length(), outfc);
            }
        } finally {
            try {
                if (infc != null) {
                    infc.close();
                }
            } catch (Exception ignore) {
            }
            try {
                if (outfc != null) {
                    outfc.close();
                }
            } catch (Exception ignore) {
            }
        }
        to.setExecutable(from.canExecute());
        to.setReadable(from.canRead());
        to.setWritable(from.canWrite());
    }

    /**
     * Class of two int fields
     */
    public static class Pair {

        public Pair(int a, int b) {
            this.a = a;
            this.b = b;
        }
        public int a;
        public int b;
    }

    public static void addToClasspath(String[] sourcePaths) {
        if (ClassLoader.getSystemClassLoader() instanceof URLClassLoader) {
            URLClassLoader systemClassLoader = (URLClassLoader) ClassLoader.getSystemClassLoader();
            Class sClass = URLClassLoader.class;
            try {
                Method method = sClass.getDeclaredMethod("addURL", new Class[]{URL.class});
                method.setAccessible(true);

                URL[] urls = new URL[sourcePaths.length];
                for (int i = 0; i < sourcePaths.length; i++) {
                    urls[i] = new File(sourcePaths[i]).toURI().toURL();
                    method.invoke(systemClassLoader, new Object[]{urls[i]});
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            // Java 9+
            String[] classpath = System.getProperty("java.class.path").split(File.pathSeparator);
            // eliminate paths that could be missed in classpath
            //1. For class files in a(n) (un)named package, the class path ends
            //   with the directory that contains the class files. (should be in classpath)
            //2. jar,zip,war should be a part of classpath (should be in classpath)
            List<String> paths = new ArrayList<>();
            Arrays.stream(sourcePaths).
                    forEach(
                            path -> {
                                File file = Paths.get(path).toFile();
                                if (file.isDirectory()) {
                                    if(!file.getName().equalsIgnoreCase("jmods")) {
                                        paths.add(path);
                                    }
                                } else if (file.isFile()) {
                                    if( FILE_TYPE.hasExtension(path, FILE_TYPE.ZIP, FILE_TYPE.JAR, FILE_TYPE.WAR) ) {
                                        paths.add(path);
                                    }
                                }
                            }
                    );
            for (String path : paths) {
                if ( !Arrays.stream(classpath).anyMatch(cp -> cp.equals(path)) ) {
                    String cps = paths.stream().collect(Collectors.joining("#"));
                    String s1 = cps.replaceAll("#", ":");
                    String s2 = cps.replaceAll("#", " ");
                    System.err.format("Warning: Add input source(s) to the classpath: -cp jcov.jar:%s%n" +
                                    "Example: java -cp jcov.jar:%s ToolName -t <template> -o <output> %s%n",
                            s1, s1, s2);
                    break;
                }
            }
        }
    }

    /**
     * Adds all classes and jars from specified directory to classpath
     *
     * @param directory - directory to find all jars and classes
     */
    public static void addToClasspath(File directory) {
        if (directory.exists() && directory.isDirectory()) {
            ArrayList<String> classes = new ArrayList<>();
            getClassesAndJars(directory, classes);
            Utils.addToClasspath(classes.toArray(new String[]{}));
        }
    }

    private static void getClassesAndJars(File dir, List<String> classes) {
        for (File f : dir.listFiles()) {
            if (f.isDirectory()) {
                getClassesAndJars(f, classes);
            } else {
                if (FILE_TYPE.hasExtension(f.getAbsolutePath(), FILE_TYPE.JAR, FILE_TYPE.CLASS) ) {
                    classes.add(f.getAbsolutePath());
                }
            }
        }
    }

    public static void setLoggerHandler(Handler loggerHandler) {
        loggerHandler.setFormatter(LoggerHandlerDelegator.formatter);
        if (Utils.loggerHandler != null) {
            loggerHandler.setLevel(Utils.loggerHandler.getLevel());
        }
        Utils.loggerHandler = loggerHandler;
    }

    public static void setLoggingLevel(Level level) {
        logger.setLevel(level);
        loggerHandler.setLevel(level);
    }

    public static void setLoggingLevel(String levelStr) {
        Level level;
        levelStr = levelStr.toUpperCase();
        if ("VERBOSE".equals(levelStr) || "V".equals(levelStr)) {
            level = Level.INFO;
        } else if ("QUIET".equals(levelStr) || "Q".equals(levelStr)) {
            level = Level.OFF;
        } else {
            level = Level.parse(levelStr);
        }
        setLoggingLevel(level);
    }

    public static class LoggerHandlerDelegator extends Handler {

        private static LoggingFormatter formatter = new LoggingFormatter();

        @Override
        public Formatter getFormatter() {
            return formatter;
        }

        @Override
        public synchronized void setLevel(Level newLevel) throws SecurityException {
            Utils.loggerHandler.setLevel(newLevel);
        }

        @Override
        public void publish(LogRecord record) {
            Utils.loggerHandler.publish(record);
        }

        @Override
        public void flush() {
            Utils.loggerHandler.flush();
        }

        @Override
        public void close() throws SecurityException {
            Utils.loggerHandler.close();
        }
    }

    public static void initLogger() {
        if (loggerHandler == null) {
            setLoggerHandler(new ConsoleHandler());
        }
        if (System.getProperty("java.util.logging.config.file") == null) {
            try (InputStream in = Utils.class.getResourceAsStream("/com/sun/tdk/jcov/logging.properties")) {
                if (ClassLoader.getSystemClassLoader().equals(Utils.class.getClassLoader())) {
                    LogManager.getLogManager().readConfiguration(in);
                } else {
                    LogManager.getLogManager().reset();
                }
            } catch (Exception ignore) {}
        }
    }

    public static void initLogger(String propfile) {
        if (System.getProperty("java.util.logging.config.file") == null) {
            System.setProperty("java.util.logging.config.file", propfile);

            try (InputStream in = Utils.class.getResourceAsStream(propfile)) {
                LogManager.getLogManager().readConfiguration(in);
            } catch (Exception ignore) {}
        }
    }

    /**
     * <p> Convert JVM type notation (as described in the JVM II 4.3.2, p.100)
     * to JLS type notation : </p> <ul> <li/>[&lt;s&gt; -> &lt;s&gt;[] &lt;s&gt;
     * is converted recursively <li/>L&lt;s&gt;; -> &lt;s&gt; characters '/' are
     * replaced by '.' in &lt;s&gt; <li/>B -> byte <li/>C -> char <li/>D ->
     * double <li/>F -> float <li/>I -> int <li/>J -> long <li/>S -> short
     * <li/>Z -> boolean <li/>V -> void valid only in method return type </ul>
     *
     * @param s VM signature string to convert
     * @return JLS signature string
     */
    public static String convertVMType(String s) {
        return getTypeName(s.replace('/', '.'));
    }

    /**
     * @param typeName
     * @return convert JVM type to JLS notation (including arrays and links)
     */
    public static String getTypeName(String typeName) {

        StringBuilder sb = new StringBuilder();

        int dims = 0;
        while (typeName.charAt(dims) == '[') {
            dims++;
        }

        String type;

        if (typeName.charAt(dims) == 'L') {
            type = typeName.substring(dims + 1, typeName.length() - 1);
        } else {
            type = PrimitiveTypes.getPrimitiveType(typeName.charAt(dims));
        }

        sb.append(type);
        for (int i = 0; i < dims; ++i) {
            sb.append("[]");
        }

        return sb.toString();
    }

    static enum PrimitiveTypes {

        BOOLEAN('Z', "boolean"), VOID('V', "void"), INT('I', "int"), LONG('J', "long"), CHAR('C', "char"), BYTE('B', "byte"), DOUBLE('D', "double"), SHORT('S', "short"), FLOAT('F', "float");
        private char VMSig;
        private String JLS;

        private PrimitiveTypes(char VMSig, String JLS) {
            this.VMSig = VMSig;
            this.JLS = JLS;
        }

        public static String getPrimitiveType(char c) {
            for (PrimitiveTypes type : PrimitiveTypes.values()) {
                if (type.VMSig == c) {
                    return type.JLS;
                }
            }
            return null;
        }

        public static Character getPrimitiveType(String str) {
            for (PrimitiveTypes type : PrimitiveTypes.values()) {
                if (type.JLS.equals(str)) {
                    return type.VMSig;
                }
            }
            return null;
        }
    }

    /**
     * Convert JVM method signature to JLS format
     *
     * @param methName
     * @param VMsig
     * @return formatted method signature (ret name(arg1,arg2,arg3))
     */
    public static String convertVMtoJLS(String methName, String VMsig) {
        Matcher m = java.util.regex.Pattern.compile("\\(([^\\)]*)\\)(.*)").matcher(VMsig);
        if (m.matches()) {
            return String.format("%s %s(%s)", convertVMType(m.group(2)), methName, getArgs(m.group(1)));
        } else {
            return methName + " " + VMsig; // some problem occured
        }
    }

    private static String getArgs(String descr) throws IllegalArgumentException {

        if (descr.equals("")) {
            return descr;
        }

        int pos = 0;
        int lastPos = descr.length();

        String type;
        StringBuilder args = new StringBuilder();

        int dims = 0;

        while (pos < lastPos) {

            char ch = descr.charAt(pos);

            if (ch == 'L') {
                int delimPos = descr.indexOf(';', pos);
                if (delimPos == -1) {
                    delimPos = lastPos;
                }
                type = convertVMType(descr.substring(pos, delimPos + 1));
                pos = delimPos + 1;
            } else if (ch == '[') {
                dims++;
                pos++;
                continue;
            } else {
                type = PrimitiveTypes.getPrimitiveType(ch);
                pos++;
            }

            args.append(type);

            for (int i = 0; i < dims; ++i) {
                args.append("[]");
            }
            dims = 0;

            if (pos < lastPos) {
                args.append(',');
            }
        }

        return args.toString();
    }

    public static Pattern[] concatFilters(String[] includes, String[] excludes){
        return concatFilters(includes, excludes, false);
    }

    public static Pattern[] concatModuleFilters(String[] includes, String[] excludes){
        return concatFilters(includes, excludes, true);
    }

    private static Pattern[] concatFilters(String[] includes, String[] excludes, boolean modulePattern) {
        if (includes == null || includes.length == 1 && includes[0].equals("")) {
            includes = new String[0];
        }
        if (excludes == null || excludes.length == 1 && excludes[0].equals("")) {
            excludes = new String[0];
        }
        Pattern alls[];
        ArrayList<Pattern> list = new ArrayList<Pattern>(includes.length + excludes.length);
        for (int i = 0; i < includes.length; ++i) {
            if (includes[i].contains("|")) {
                String[] split = includes[i].split("\\|");
                for (int j = 0; j < split.length; ++j) {
                    list.add(new Pattern(split[j], true, modulePattern));
                }
            } else {
                list.add(new Pattern(includes[i], true, modulePattern));
            }
        }
        for (int i = 0; i < excludes.length; ++i) {
            if (excludes[i].contains("|")) {
                String[] split = excludes[i].split("\\|");
                for (int j = 0; j < split.length; ++j) {
                    list.add(new Pattern(split[j], false, modulePattern));
                }
            } else {
                list.add(new Pattern(excludes[i], false, modulePattern));
            }
        }
        alls = list.toArray(new Pattern[list.size()]);
        java.util.Arrays.sort(alls);
        return alls;
    }

    /**
     * <p> ClassName specific pattern implementation. Patterns are compared
     * taking into account classname separators ('/' and '$' signs) as well as
     * allowing to users some simplifications ('.' as package separator instead
     * of '/', ignoring leading '/'). </p> <p> Pattern can describe inclusion or
     * exclusion -
     * <code>included</code> field. Patterns are using wildcards ('*') but it's
     * prohibited to use extended pattern syntax (eg grouping). </p>
     */
    public static class Pattern implements Comparable<Pattern> {

        public String element;
        public java.util.regex.Pattern patt;
        public boolean included;

        public String getElement() {
            return element;
        }

        public void setElement(String element) {
            this.element = element;
        }

        public boolean isIncluded() {
            return included;
        }

        public void setIncluded(boolean included) {
            this.included = included;
        }

        /**
         *
         * @param element Should not be null
         * @param include
         */
        public Pattern(String element, boolean include, boolean modulePattern) {
            try {
                if ("/".equals(element)) {
                    this.element = element;
                    this.included = include;
                    this.patt = java.util.regex.Pattern.compile("/[a-zA-Z0-9_\\$]+");
                } else {
                    if (modulePattern) {
                        this.element = element.replaceAll("([^\\\\])\\$", "$1\\\\\\$");
                    }
                    else{
                        this.element = element.replaceAll("\\.", "/").replaceAll("([^\\\\])\\$", "$1\\\\\\$");
                    }
                    if (this.element.endsWith("/")) {
                        this.element = this.element.substring(0, this.element.length() - 1);
                    }
                    if (!modulePattern) {
                        if (this.element.length() == 0 || !this.element.startsWith("/")) {
                            this.element = "/" + this.element;
                        }
                    }
                    this.patt = java.util.regex.Pattern.compile(this.element.replace('*', '#').replaceAll("##", "[a-zA-Z0-9_\\$/]*").replaceAll("#", "[a-zA-Z0-9_\\$]*") + "(/.*|\\$.*)*");
                    this.included = include;
                }
            } catch (PatternSyntaxException e) {
                int p = element.indexOf('\\');
                if (p > -1) {
                    throw new IllegalArgumentException("Illegal character '\\' in the pattern '" + element + "' near index " + p);
                }
                p = element.indexOf('(');
                if (p > -1) {
                    throw new IllegalArgumentException("Illegal character '(' in the pattern '" + element + "' near index " + p);
                }
                p = element.indexOf(')');
                if (p > -1) {
                    throw new IllegalArgumentException("Illegal character ')' in the pattern '" + element + "' near index " + p);
                }
                p = element.indexOf('[');
                if (p > -1) {
                    throw new IllegalArgumentException("Illegal character '[' in the pattern '" + element + "' near index " + p);
                }
                p = element.indexOf(']');
                if (p > -1) {
                    throw new IllegalArgumentException("Illegal character ']' in the pattern '" + element + "' near index " + p);
                }
                throw new IllegalArgumentException("Illegal characters in the pattern '" + element + "'");
            }
        }

        @Override
        public String toString() {
            return (included ? "+" : "-") + element;
        }

        public int compareTo(Pattern o) {
            int i = 0, j = 0;
            if (element.contains("$")) {
                if (!o.element.contains("$")) {
                    return 1;
                }
            } else {
                if (o.element.contains("$")) {
                    return -1;
                }
            }
            char my[] = element.toCharArray();
            char his[] = o.element.toCharArray();
            for (; i < my.length && j < his.length; ++i, ++j) {
                char me = my[i];
                char him = his[i];
                if (me == '/' ^ him == '/') { // if only one of the strings finished ("/com/su/tdk" VS "/com/sun/tdk" or otherwise) - checking. If both - they are OK
                    if (me == '/') { // "/com/su/tdk" VS "/com/sun/tdk": this is lesser
                        return -1;
                    }
                    // else
                    return 1;// "/com/sunn/tdk" VS "/com/sun/tdk": this is bigger
                }
                int res = me - him;
                if (res != 0) {
                    if (me == '*') {
                        if (i < my.length - 1 && my[i + 1] == '*') {
                            return -2;
                        }
                        return -1;
                    }
                    if (him == '*') {
                        if (i < his.length - 1 && his[i + 1] == '*') {
                            return 2;
                        }
                        return 1;
                    }
                    return res;
                } else {
                    if (me == '*' && i < my.length - 1 && my[i + 1] == '*') {
                        return 2;
                    } else if (him == '*' && i < his.length - 1 && his[i + 1] == '*') {
                        return -2;
                    }
                }
            }
            if (i == my.length && j == his.length) { // two strings are equal
                if (included == o.included) {
                    return 0;
                } else {
                    if (included) {
                        return 1;
                    }
                    if (o.included) {
                        return -1;
                    }
                    return 0;
                }
            }
            if (i == my.length) { // "/com/sun" VS "/com/sun/tdk" - this = parent
                return -1000;
            }
            if (j == his.length) { // "/com/sun/tdk" VS "/com/sun" - this = child
                return 1000;
            }
            return 0; // should not happen
        }
    }

    /**
     * <p> Check whether a
     * <code>className</code> is accepted by patterns. </p>
     *
     * @param alls patterns to use
     * @param allowedModifs class modificators to accept. Ignored if null.
     * @param className class name to check
     * @param sig class's signature
     * @return true if a class name is accepted by all patters and contain at
     * least one allowed modificator
     */
    public static boolean accept(Pattern[] alls, String[] allowedModifs, String className, String sig) {
        if (alls.length == 0) {
            if (allowedModifs != null && sig != null && allowedModifs.length != 0) {
                for (int j = 0; j < allowedModifs.length; j++) {
                    if (sig.contains(allowedModifs[j])) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        Boolean parentIncluded = null; // null means that no pattern matching the className found - neither including or excluding
        boolean included = false;
//        System.out.println("c: " + className);
        outer:
        for (int i = 0; i < alls.length; ++i) {
            if (alls[i].included) {
                included = true;
            }

//            System.out.println("match: " + alls[i].patt.toString());
            if (alls[i].patt.matcher(className).matches()) {
//                System.out.println("matched " + alls[i].included);
                parentIncluded = alls[i].included;
            }
        }

        if (sig == null || allowedModifs == null || allowedModifs.length == 0) {
            return parentIncluded != null ? parentIncluded : !included; // if parent package was not found (parentIncluded == null) - accept depends on was ANYTHING included or not
        }

        for (int j = 0; j < allowedModifs.length; j++) {
            if (sig.contains(allowedModifs[j])) {
                return true;
            }
        }
        return false;
    }

    public enum CheckOptions {
        FILE_EXISTS,
        FILE_NOTEXISTS,
        FILE_ISFILE,
        FILE_ISDIR,
        FILE_NOTISDIR,
        FILE_PARENTEXISTS,
        FILE_CANREAD,
        FILE_CANWRITE,
        INT_NONNEGATIVE,
        INT_POSITIVE,
        INT_NOT_NULL,
        FILE_NOTISFILE
    }

    /**
     * <p> Checks that hostname is valid </p>
     *
     * @param hostname Hostname to check
     * @param description Hostname description (eg "remote server address")
     * @throws com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException
     */
    public static void checkHostCanBeNull(String hostname, String description) throws EnvHandlingException {
        if (hostname != null) {
            try {
                InetAddress.getByName(hostname);
            } catch (UnknownHostException ex) {
                throw new EnvHandlingException("Incorrect " + description + " (" + hostname + ") - unknown host, can't resolve");
            }
        }
    }

    /**
     * <p> Checks a file for some criterias </p>
     *
     * @param filename File to check
     * @param description File description (eg "output directory")
     * @param opts criterias to check
     * @return File object related with <code>filename</code>
     * @throws com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException
     */
    public static File checkFileNotNull(String filename, String description, CheckOptions... opts) throws EnvHandlingException {
        if (filename == null) {
            throw new IllegalArgumentException("Argument " + description + " should not be null");
        }
        final File f = new File(filename);
        checkFile(f, description, opts);
        return f;
    }

    /**
     * <p> Checks a file for some criterias </p>
     *
     * @param filename File to check
     * @param description File description (eg "output directory")
     * @param opts criterias to check
     * @return File object related with <code>filename</code> or null
     * @throws com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException
     */
    public static File checkFileCanBeNull(String filename, String description, CheckOptions... opts) throws EnvHandlingException {
        if (filename == null) {
            return null;
        }
        final File f = new File(filename);
        checkFile(f, description, opts);
        return f;
    }

    /**
     * <p> Checks a file for some criteria </p>
     *
     * @param file File to check. Can't be null.
     * @param description File description
     * @param opts criteria to check
     * @throws com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException
     */
    public static void checkFile(File file, String description, CheckOptions... opts) throws EnvHandlingException {
        for (CheckOptions opt : opts) {
            switch (opt) {
                case FILE_EXISTS:
                    if (!file.exists()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - doesn't exist");
                    }
                    break;
                case FILE_NOTEXISTS:
                    if (file.exists()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - shouldn't exist");
                    }
                    break;
                case FILE_CANREAD:
                    if (!file.canRead()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - can't read");
                    }
                    break;
                case FILE_CANWRITE:
                    if (!file.canWrite()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - can't write");
                    }
                    break;
                case FILE_ISFILE:
                    if (!file.isFile()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - is not a file");
                    }
                    break;
                case FILE_NOTISFILE:
                    if (file.isFile()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - should not be a file");
                    }
                    break;
                case FILE_ISDIR:
                    if (!file.isDirectory()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - is not a directory");
                    }
                    break;
                case FILE_NOTISDIR:
                    if (file.isDirectory()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - should not be a directory");
                    }
                    break;
                case FILE_PARENTEXISTS:
                    File p = file.getParentFile();
                    if (p != null && !p.exists()) {
                        throw new EnvHandlingException("Incorrect " + description + " (" + file.getPath() + ") - parent directory doesn't exist");
                    }
                    break;
            }
        }
    }

    /**
     * Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname or
     * empty list if the abstract pathname does not denote a directory
     *
     * @param dir abstract pathname denotes a directory
     */
    public static List<File> getListFiles(File dir) {
        ArrayList<File> listFiles = new ArrayList<>();
        File[] list = dir.listFiles();
        if( list != null && list.length > 0) {
            listFiles.addAll(Arrays.asList(list));
        }
        return listFiles;
    }

    /**
     * <p> Converts a string to integer using Integer.parseInt() and checks some
     * criterias </p>
     *
     * @param value String to convert
     * @param description value description (eg "port number")
     * @param opts criterias to check
     * @return integer value
     * @throws com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException
     */
    public static int checkedToInt(String value, String description, CheckOptions... opts) throws EnvHandlingException {
        try {
            int port = Integer.parseInt(value);
            for (CheckOptions opt : opts) {
                switch (opt) {
                    case INT_POSITIVE:
                        if (port <= 0) {
                            throw new EnvHandlingException("Incorrect " + description + " (" + port + ") - should be positive");
                        }
                        break;
                    case INT_NONNEGATIVE:
                        if (port < 0) {
                            throw new EnvHandlingException("Incorrect " + description + " (" + port + ") - should not be negative");
                        }
                        break;
                    case INT_NOT_NULL:
                        if (port == 0) {
                            throw new EnvHandlingException("Incorrect " + description + " (0) - should not be null");
                        }
                        break;
                }
            }
            return port;
        } catch (NumberFormatException ex) {
            throw new EnvHandlingException("Incorrect " + description + " (" + value + ") - not a number");
        }
    }

    static public void unzipFolder(File srcFolder, String destZipFile) throws Exception {

        ZipFile zip = new ZipFile(srcFolder);
        Enumeration zipFileEntries = zip.entries();

        while (zipFileEntries.hasMoreElements()) {

            ZipEntry entry = (ZipEntry) zipFileEntries.nextElement();
            File destFile = new File(destZipFile, entry.getName());
            destFile.getParentFile().mkdirs();

            if (!entry.isDirectory()) {
                BufferedInputStream is = new BufferedInputStream(zip.getInputStream(entry));
                FileOutputStream fos = new FileOutputStream(destFile);
                BufferedOutputStream dest = new BufferedOutputStream(fos, 2048);

                int currentByte;
                byte data[] = new byte[2048];
                while ((currentByte = is.read(data, 0, 2048)) != -1) {
                    dest.write(data, 0, currentByte);
                }
                dest.flush();
                dest.close();
                is.close();
            }

        }
    }

    /**
     * Adds directory to zip archive
     *
     * @param srcFolder directory to zip
     * @param destZipFile result zip file
     * @throws Exception exceptions while working with file system
     */
    static public void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        addFolderToZip("", srcFolder, zip);
        zip.flush();
        zip.close();
    }

    static private void addFileToZip(String path, String srcFile, ZipOutputStream zip)
            throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + File.separator + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
            in.close();
        }
    }

    static private void addFolderToZip(String path, String srcFolder, ZipOutputStream zip)
            throws Exception {
        File folder = new File(srcFolder);

        for (String fileName : folder.list()) {
            if (path.equals("")) {
                addFileToZip(folder.getName(), srcFolder + File.separator + fileName, zip);
            } else {
                addFileToZip(path + File.separator + folder.getName(), srcFolder + File.separator + fileName, zip);
            }
        }
    }

    public static void copyDirectory(File source, File destination) throws IOException {
        if (!source.exists()) {
            throw new IllegalArgumentException("Source directory (" + source.getPath() + ") doesn't exist.");
        }

        if (!source.isDirectory()) {
            throw new IllegalArgumentException("Source (" + source.getPath() + ") must be a directory.");
        }

        destination.mkdirs();

        File[] files = source.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                copyDirectory(file, new File(destination, file.getName()));
            } else {
                copyFile(file, new File(destination, file.getName()));
            }
        }
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (null != files) {
                for (int i = 0; i < files.length; i++) {
                    if (files[i].isDirectory()) {
                        deleteDirectory(files[i]);
                    } else {
                        files[i].delete();
                    }
                }
            }
        }
        return (directory.delete());
    }

    private static String checkName(String name) {
        if (name != null && !name.isEmpty()) {
            return name;
        }
        return "";
    }

    public static boolean isAdvanceStaticInstrAllowed(String classname, String methodname) {
        if ((!classname.equals("java/lang/System"))
                && (!classname.equals("java/lang/String"))
                && (!classname.equals("java/lang/StringLatin1"))
                && (!classname.equals("java/lang/String$CaseInsensitiveComparator"))
                && (!classname.equals("java/lang/Thread"))
                && (!classname.equals("java/lang/ThreadGroup"))
                && (!classname.equals("java/lang/RuntimePermission"))
                && (!classname.equals("java/security/Permission"))
                && (!classname.equals("java/security/BasicPermission"))
                && (!classname.equals("java/lang/Class"))) {
            return true;
        }
        if (!methodname.equals("<clinit>")
                && !methodname.equals("<init>")
                && !methodname.equals("init")
                && !methodname.equals("length")
                && !methodname.equals("coder")
                && !methodname.equals("isLatin1")
                && !methodname.equals("charAt")
                && !methodname.equals("equals")
                && !methodname.equals("add")
                && !methodname.equals("checkAccess")
                && !methodname.equals("checkParentAccess")
                && !methodname.equals("getSecurityManager")
                && !methodname.equals("allowSecurityManager")
                && !methodname.equals("registerNatives")
                && !methodname.equals("currentTimeMillis")
                && !methodname.equals("identityHashCode")
                && !methodname.equals("nanoTime")
                && !methodname.equals("getId")) {
            return true;
        }
        return false;
    }
}
