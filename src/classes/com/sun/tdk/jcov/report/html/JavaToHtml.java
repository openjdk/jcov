/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.tdk.jcov.report.html;

/**
 * CodeViewer.java
 *
 * Bill Lynch & Matt Tucker CoolServlets.com, October 1999
 *
 * Please visit CoolServlets.com for high quality, open source Java servlets.
 *
 * Copyright (C) 1999 CoolServlets.com
 *
 * Any errors or suggested improvements to this class can be reported as
 * instructed on Coolservlets.com. We hope you enjoy this program... your
 * comments will encourage further development!
 *
 * This software is distributed under the terms of The BSD License.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. * Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither name of CoolServlets.com nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY COOLSERVLETS.COM AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL COOLSERVLETS.COM OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import java.util.HashMap;

/**
 *
 * @since 1.0
 */
public class JavaToHtml {

    private static HashMap reservedWords = new HashMap();
    private static boolean inMultiLineComment = false;
    private static String commentStart = "<span Class=\"comment\">";
    private static String commentEnd = "</span>";
    private static String stringStart = "<span Class=\"string\">";
    private static String stringEnd = "</span>";
    private static String reservedWordStart = "<span Class=\"keyword\">";
    private static String reservedWordEnd = "</span>";

    static {
        loadHash();
    }

    /**
     * Passes off each line to the first filter.
     *
     * @param line The line of Java code to be highlighted.
     * @return Highlighted line.
     */
    public static String syntaxHighlight(String line) {
        return htmlFilter(line);
    }

    /*
     * Filter html tags into more benign text.
     */
    private static String htmlFilter(String line) {
        if (line == null || line.equals("")) {
            return "";
        }

        // replace ampersands with HTML escape sequence for ampersand;
        line = replace(line, "&", "&#38;");

        // replace the \\ with HTML escape sequences. fixes a problem when
        // backslashes proceed quotes.
        line = replace(line, "\\\\", "&#92;&#92;");

        // replace \" sequences with HTML escape sequences;
        line = replace(line, "" + (char) 92 + (char) 34, "&#92;&#34");

        // replace less-than signs which might be confused
        // by HTML as tag angle-brackets;
        line = replace(line, "<", "&#60;");
        // replace greater-than signs which might be confused
        // by HTML as tag angle-brackets;
        line = replace(line, ">", "&#62;");

        return multiLineCommentFilter(line);
    }

    /*
     * Filter out multiLine comments. State is kept with a private boolean
     * variable.
     */
    private static String multiLineCommentFilter(String line) {
        if (line == null || line.equals("")) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        int index;
        //First, check for the end of a multi-line comment.
        if (inMultiLineComment && (index = line.indexOf("*/")) > -1 && !isInsideString(line, index)) {
            inMultiLineComment = false;
            buf.append(commentStart);
            buf.append(line.substring(0, index));
            buf.append("*/").append(commentEnd);
            if (line.length() > index + 2) {
                buf.append(inlineCommentFilter(line.substring(index + 2)));
            }
            return buf.toString();
        } //If there was no end detected and we're currently in a multi-line
        //comment, we don't want to do anymore work, so return line.
        else if (inMultiLineComment) {
            buf.append(commentStart);
            buf.append(line);
            buf.append(commentEnd);
            return buf.toString();
        } //We're not currently in a comment, so check to see if the start
        //of a multi-line comment is in this line.
        else if ((index = line.indexOf("/*")) > -1 && !isInsideString(line, index)) {
            inMultiLineComment = true;
            //Return result of other filters + everything after the start
            //of the multiline comment. We need to pass the through the
            //to the multiLineComment filter again in case the comment ends
            //on the same line.
            buf.append(inlineCommentFilter(line.substring(0, index)));
            buf.append(commentStart).append("/*");
            buf.append(multiLineCommentFilter(line.substring(index + 2)));
            buf.append(commentEnd);
            return buf.toString();
        } //Otherwise, no useful multi-line comment information was found so
        //pass the line down to the next filter for processing.
        else {
            return inlineCommentFilter(line);
        }
    }

    /*
     * Filter inline comments from a line and formats them properly.
     */
    private static String inlineCommentFilter(String line) {
        if (line == null || line.equals("")) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        int index;
        if ((index = line.indexOf("//")) > -1 && !isInsideString(line, index)) {
            buf.append(stringFilter(line.substring(0, index)));
            buf.append(commentStart);
            buf.append(line.substring(index));
            buf.append(commentEnd);
        } else {
            buf.append(stringFilter(line));
        }
        return buf.toString();
    }

    /*
     * Filters strings from a line of text and formats them properly.
     */
    private static String stringFilter(String line) {
        if (line == null || line.equals("")) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        if (line.indexOf("\"") <= -1) {
            return keywordFilter(line);
        }
        int start = 0;
        int startStringIndex = -1;
        int endStringIndex = -1;
        int tempIndex;
        //Keep moving through String characters until we want to stop...
        while ((tempIndex = line.indexOf("\"")) > -1) {
            //We found the beginning of a string
            if (startStringIndex == -1) {
                startStringIndex = 0;
                buf.append(stringFilter(line.substring(start, tempIndex)));
                buf.append(stringStart).append("\"");
                line = line.substring(tempIndex + 1);
            } //Must be at the end
            else {
                startStringIndex = -1;
                endStringIndex = tempIndex;
                buf.append(line.substring(0, endStringIndex + 1));
                buf.append(stringEnd);
                line = line.substring(endStringIndex + 1);
            }
        }

        buf.append(keywordFilter(line));

        return buf.toString();
    }

    /*
     * Filters keywords from a line of text and formats them properly.
     */
    private static String keywordFilter(String line) {
        if (line == null || line.equals("")) {
            return "";
        }
        StringBuffer buf = new StringBuffer();
        HashMap usedReservedWords = new HashMap(); // >= Java2 only (not thread-safe)
        //Hashtable usedReservedWords = new Hashtable(); // < Java2 (thread-safe)
        int i = 0, startAt = 0;
        char ch;
        StringBuffer temp = new StringBuffer();
        while (i < line.length()) {
            temp.setLength(0);
            ch = line.charAt(i);
            startAt = i;
            // 65-90, uppercase letters
            // 97-122, lowercase letters
            while (i < line.length() && ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_')) {
                temp.append(ch);
                i++;
                if (i < line.length()) {
                    ch = line.charAt(i);
                }
            }
//            System.out.println("found " + temp.toString());
            String tempString = temp.toString();
            if (reservedWords.containsKey(tempString) && !usedReservedWords.containsKey(tempString)) {
//                usedReservedWords.put(tempString, tempString);
                line = replace(line, tempString, (reservedWordStart + tempString + reservedWordEnd), startAt, true);
//                line = replace(line, "[^_a-zA-Z0-9]" + tempString + "[^_a-zA-Z0-9]", (reservedWordStart + tempString + reservedWordEnd), startAt);
                i += (reservedWordStart.length() + reservedWordEnd.length());
            } else {
                i++;
            }
        }
        buf.append(line);
        return buf.toString();
    }

    /*
     * All important replace method. Replaces all occurrences of oldString in
     * line with newString.
     */
    private static String replace(String line, String oldString, String newString) {
        return replace(line, oldString, newString, 0, false);
    }

    /*
     * All important replace method. Replaces all occurrences of oldString in
     * line with newString.
     */
    private static String replace(String line, String oldString, String newString, int startAt, boolean once) {
        int i = startAt;
        while ((i = line.indexOf(oldString, i)) >= 0) {
            line = (new StringBuffer().append(line.substring(0, i))
                    .append(newString)
                    .append(line.substring(i + oldString.length()))).toString();
            i += newString.length();
            if (once) {
                break; // workarounding "if (ident_if_ier)" problem
            }
        }
        return line;
    }

    /*
     * Checks to see if some position in a line is between String start and
     * ending characters. Not yet used in code or fully working :)
     */
    private static boolean isInsideString(String line, int position) {
        if (line.indexOf("\"") < 0) {
            return false;
        }
        int index;
        String left = line.substring(0, position);
        String right = line.substring(position);
        int leftCount = 0;
        int rightCount = 0;
        while ((index = left.indexOf("\"")) > -1) {
            leftCount++;
            left = left.substring(index + 1);
        }
        while ((index = right.indexOf("\"")) > -1) {
            rightCount++;
            right = right.substring(index + 1);
        }
        if (rightCount % 2 != 0 && leftCount % 2 != 0) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Load Hashtable (or HashMap) with Java reserved words.
     */
    private static void loadHash() {
        reservedWords.put("abstract", "abstract");
        reservedWords.put("assert", "assert");
        reservedWords.put("boolean", "boolean");
        reservedWords.put("break", "break");
        reservedWords.put("byte", "byte");
        reservedWords.put("case", "case");
        reservedWords.put("catch", "catch");
        reservedWords.put("char", "char");
        reservedWords.put("class", "class");
        reservedWords.put("const", "const");
        reservedWords.put("continue", "continue");
        reservedWords.put("default", "default");
        reservedWords.put("do", "do");
        reservedWords.put("double", "double");
        reservedWords.put("else", "else");
        reservedWords.put("enum", "enum");
        reservedWords.put("extends", "extends");
        reservedWords.put("false", "false");
        reservedWords.put("final", "final");
        reservedWords.put("finally", "finally");
        reservedWords.put("float", "float");
        reservedWords.put("for", "for");
        reservedWords.put("goto", "goto");
        reservedWords.put("if", "if");
        reservedWords.put("implements", "implements");
        reservedWords.put("import", "import");
        reservedWords.put("instanceof", "instanceof");
        reservedWords.put("int", "int");
        reservedWords.put("interface", "interface");
        reservedWords.put("long", "long");
        reservedWords.put("native", "native");
        reservedWords.put("new", "new");
        reservedWords.put("null", "null");
        reservedWords.put("package", "package");
        reservedWords.put("private", "private");
        reservedWords.put("protected", "protected");
        reservedWords.put("public", "public");
        reservedWords.put("return", "return");
        reservedWords.put("short", "short");
        reservedWords.put("static", "static");
        reservedWords.put("strictfp", "strictfp");
        reservedWords.put("super", "super");
        reservedWords.put("switch", "switch");
        reservedWords.put("synchronized", "synchronized");
        reservedWords.put("this", "this");
        reservedWords.put("throw", "throw");
        reservedWords.put("throws", "throws");
        reservedWords.put("transient", "transient");
        reservedWords.put("true", "true");
        reservedWords.put("try", "try");
        reservedWords.put("void", "void");
        reservedWords.put("volatile", "volatile");
        reservedWords.put("while", "while");
    }
}
