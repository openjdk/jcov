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
package com.sun.tdk.jcov.ant;

import com.sun.tdk.jcov.Merger;
import static com.sun.tdk.jcov.Merger.BreakOnError;
import com.sun.tdk.jcov.data.Result;
import com.sun.tdk.jcov.util.Utils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.apache.tools.ant.types.resources.FileResource;

/**
 *
 * @author Andrey Titov
 */
public class Merge extends Task {

    private LinkedList<FileSet> sources;
    private LinkedList<JCovFile> results;
    private PatternSet filter;
    private File output;
    private File testList;
    private int looseLevel;
    private boolean compressScales;
    private String breakOnError;
    private File template;
    private File skippedList;
    private File fileList;
    private boolean generateScales;
    public File fmlist;
    public LinkedList<FM> fm = new LinkedList<FM>();

    public Merge() {
        results = new LinkedList<JCovFile>();
        sources = new LinkedList<FileSet>();
    }

    @Override
    public void execute() throws BuildException {

        Merger merger = new Merger();
        merger.resetDefaults();

        String fms[] = merger.getFm();
        if (fmlist != null) {
            try {
                String[] readLines = Utils.readLines(fmlist.getPath());
                for (String s : readLines) {
                    fm.add(new FM(s));
                }
            } catch (IOException ex) {
                throw new BuildException("Unable to read fmlist", ex);
            }
        }
        int fmsize = fm.size();
        if (fmsize > 0) {
            fms = new String[fmsize];
            int i = 0;
            for (FM f : fm) {
                fms[i++] = f.value;
            }
            merger.setClassModifiers(fms);
        }
        if (filter != null) {
            String incl[] = merger.getInclude();
            String excl[] = merger.getExclude();
            if (filter.getExcludePatterns(getProject()) != null) {
                excl = filter.getExcludePatterns(getProject());
            }
            if (filter.getIncludePatterns(getProject()) != null) {
                incl = filter.getIncludePatterns(getProject());
            }
            merger.setFilters(incl, excl, fms);
        }

        BreakOnError boe = Merger.BreakOnError.fromString(breakOnError);
        if (boe == null) {
            boe = BreakOnError.getDefault();
        }
        merger.setBreakOnError(boe);
        merger.setLoose_lvl(looseLevel);
        merger.setRead_scales(testList != null || generateScales);
        merger.setCompress(compressScales);

        Result files[] = null;
        LinkedList<Result> paths = new LinkedList<Result>();
        for (JCovFile file : results) {
            paths.add(file.getResult());
        }
        for (FileSet fs : sources) {
            Iterator it = fs.iterator();
            while (it.hasNext()) {
                Object next = it.next();
                if (next instanceof FileResource) {
                    FileResource file = (FileResource) next;
                    paths.add(new Result(file.getFile().getAbsolutePath()));
                } else {
                    // can be?
                }
            }
        }
        if (fileList != null) {
            if (!fileList.exists() || !fileList.isFile()) {
                log("WARNING: filelist " + fileList + " doesn't exist");
            } else {
                try {
                    String[] filesInList = Utils.readLines(fileList.getPath());
                    for (String f : filesInList) {
                        paths.add(Merger.parseResultFromString(f));
                    }
                } catch (IOException ex) {
                    throw new BuildException("Error while parsing filelist", ex);
                }
            }
        }
        if (paths.size() < 2) {
            if (paths.size() == 0) {
                throw new BuildException("No jcov files found to merge - need at least 2 jcov files.");
            }
            if (paths.size() == 1) {
                throw new BuildException("Need at least 2 jcov files to merge. Found 1: " + paths.getFirst().getResultPath());
            }
        }
        files = paths.toArray(new Result[paths.size()]);
        paths = null;

        Merger.Merge merge;
        if (template != null) {
            merge = new Merger.Merge(files, template.getPath());
        } else {
            merge = new Merger.Merge(files, null);
        }
        try {
            merger.mergeAndWrite(merge,
                    testList == null ? null : testList.getPath(),
                    output == null ? null : output.getPath(),
                    skippedList == null ? null : skippedList.getPath());
        } catch (IOException ex) {
            Logger.getLogger(Merge.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (merge.getErrors() > 0) {
            log("Merger produced " + merge.getErrors() + " errors while working", 2);
        }

        if (merge.getSkippedCount() > 0) {
            List<String> skippedFiles = merge.getSkippedFiles();
            log(skippedFiles.size() + (skippedFiles.size() > 1 ? " file was skipped due to errors" : " files were skipped due to errors"), 2);
            log("Skipped file" + (skippedFiles.size() > 1 ? "s: " + Arrays.toString(skippedFiles.toArray(new String[skippedFiles.size()])) : ": " + skippedFiles.get(0)), 3);
            if (skippedList != null) {
                log("Skipped files list was written to " + skippedList, 3);
            }
        }

        if (merge.getResult() == null && boe != BreakOnError.TEST) {
            throw new BuildException("Merging failed");
        }
    }

    public void setBreakOnError(String breakOnError) {
        this.breakOnError = breakOnError;
    }

    public void setCompressScales(boolean compressScales) {
        this.compressScales = compressScales;
    }

    public void setOutput(File destdir) {
        this.output = destdir;
    }

    public void setLooseLevel(int looseLevel) {
        this.looseLevel = looseLevel;
    }

    public void setSkippedList(File skippedList) {
        this.skippedList = skippedList;
    }

    public void setTemplate(File template) {
        this.template = template;
    }

    public void setTestList(File testList) {
        this.testList = testList;
    }

    public void setFileList(File fileList) {
        this.fileList = fileList;
    }

    public FileSet createFiles() {
        FileSet fs = new FileSet();
        sources.add(fs);
        return fs;
    }

    public PatternSet createFilter() throws Exception {
        if (filter != null) {
            throw new BuildException("Filter should be only one");
        }
        filter = new PatternSet();
        return filter;
    }

    public JCovFile createJcovFile() {
        JCovFile res = new JCovFile();
        results.add(res);
        return res;
    }

    public static class JCovFile {

        private String respath;
        private String[] testList;
        private int first = -1;
        private int last = -1;

        public void setPath(String path) {
            this.respath = path;
        }

        public void setTestListPath(String path) throws IOException {
            if (testList != null) {
                throw new BuildException("Test list is already set by testName attribute");
            }
            if (first >= 0 || last >= 0) {
                testList = Merger.initTestList(path, first, last);
            } else {
                testList = Merger.initTestList(path);
            }
        }

        public String[] getTestList() {
            if (testList == null) {
                String res = new File(respath).getName();
                testList = new String[]{res};
            }
            return testList;
        }

        public void setTestName(String name) {
            if (testList != null) {
                throw new BuildException("Test list is already set by testListPath attribute");
            }
            testList = new String[]{name};
        }

        private Result getResult() {
            return new Result(respath, testList);
        }

        public void setFirst(int first) {
            this.first = first;
        }

        public void setLast(int last) {
            this.last = last;
        }
    }

    public void setGenerateScales(boolean generateScales) {
        this.generateScales = generateScales;
    }

    public void setFmlist(File fmlist) throws IOException {
        String[] lines = Utils.readLines(fmlist.getPath());
        for (String s : lines) {
            fm.add(new FM(s));
        }
        this.fmlist = fmlist;
    }

    public FM createFM() {
        FM f = new FM();
        fm.add(f);
        return f;
    }

    public static class FM {

        private String value;

        public FM() {
        }

        public FM(String value) {
            this.value = value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
