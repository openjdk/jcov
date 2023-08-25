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
package openjdk.codetools.jcov.report.filter;

import openjdk.codetools.jcov.report.LineRange;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An implementation of the source filter which is coming from parsing git diff.
 * @see #parseDiff(Path)
 */
public class GitDiffFilter implements SourceFileFilter {
    private final Map<String, List<LineRange>> lines;

    private GitDiffFilter(Map<String, List<LineRange>> lines) {
        this.lines = lines;
    }

    /**
     * Parses git diff into a source filter. The diff is supposed to be produced with <code>-U0</code> option.
     * @param diffFile the File to parse
     * @return
     * @throws IOException
     */
    public static SourceFileFilter parseDiff(Path diffFile) throws IOException {
        Map<String, List<LineRange>> lines = new HashMap<>();
        try(BufferedReader in = Files.newBufferedReader(diffFile)) {
            String line = null;
            while(true) {
                if (line == null)
                    while ((line = in.readLine()) != null && !line.startsWith("+++ b/")) {}
                if (line == null) break;;
                String fileName = line.substring("+++ b/".length());
                if (fileName.endsWith(".java")) {
                    List<LineRange> fragments = new ArrayList<>();
                    String lineNumbers = in.readLine();
                    while (lineNumbers != null && !lineNumbers.startsWith("+++ b/")) {
                        lineNumbers = lineNumbers.substring("@@ ".length());
                        lineNumbers = lineNumbers.substring(lineNumbers.indexOf(" +") + 2, lineNumbers.indexOf(" @@"));
                        int commaIndex = lineNumbers.indexOf(',');
                        int firstLine, lastLine;
                        if (commaIndex > -1) {
                            firstLine = Integer.parseInt(lineNumbers.substring(0, commaIndex));
                            lastLine = firstLine + Integer.parseInt(lineNumbers.substring(commaIndex + 1)) - 1;
                        } else {
                            lastLine = firstLine = Integer.parseInt(lineNumbers);
                        }
                        fragments.add(new LineRange(firstLine, lastLine));
                        while ((lineNumbers = in.readLine()) != null && !lineNumbers.startsWith("@@ ")
                                && !lineNumbers.startsWith("+++ b/")) {
                        }
                        if (lineNumbers == null) break;
                        if (lineNumbers.startsWith("+++ b/")) {
                            line = lineNumbers;
                            continue;
                        }
                    }
                    lines.put(fileName, fragments);
                    if (lineNumbers == null) break;
                } else line = null;
            }
        }
        return new GitDiffFilter(lines);
    }

    @Override
    public Set<String> files() {
        return lines.keySet();
    }

    @Override
    public List<LineRange> ranges(String file) {
        return lines.get(file);
    }
}
