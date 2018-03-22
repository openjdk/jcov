/*
 * Copyright (c) 2018, Eric L. McCorkle. All rights reserved.
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package openjdk.codetools.jcov.plugin.coberturaxml;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Runnable;

import com.sun.tdk.jcov.instrument.XmlContext;
import com.sun.tdk.jcov.report.AbstractCoverage.CoverageFormatter;
import com.sun.tdk.jcov.report.*;

public class CoberturaReportGenerator implements ReportGenerator {
    private static final String XML_HEADER =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final String XML_DTD =
        "<!DOCTYPE coverage SYSTEM \"http://cobertura.sourceforge" +
        ".net/xml/coverage-04.dtd\">";
    private static final String COVERAGE_NODE = "coverage";
    private static final String LINE_RATE_ATTR = "line-rate";
    private static final String LINES_COVERED_ATTR = "lines-covered";
    private static final String LINES_VALID_ATTR = "lines-valid";
    private static final String BRANCH_RATE_ATTR = "branch-rate";
    private static final String BRANCHES_COVERED_ATTR = "branches-covered";
    private static final String BRANCHES_VALID_ATTR = "branches-valid";
    private static final String BRANCHES_RATE_ATTR = "branches-rate";
    private static final String TIMESTAMP_ATTR = "timestamp";
    private static final String COMPLEXITY_ATTR = "complexity";
    private static final String VERSION_ATTR = "version";

    private static final String SOURCES_NODE = "sources";
    private static final String SOURCE_NODE = "source";

    private static final String PACKAGES_NODE = "packages";

    private static final String PACKAGE_NODE = "package";

    private static final String CLASSES_NODE = "classes";

    private static final String CLASS_NODE = "class";
    private static final String NAME_ATTR = "name";
    private static final String FILENAME_ATTR = "filename";


    private static final String METHODS_NODE = "methods";

    private static final String METHOD_NODE = "method";
    private static final String SIGNATURE_ATTR = "signature";
    private static final String HITS_ATTR = "hits";

    private static final String LINES_NODE = "lines";

    private static final String LINE_NODE = "line";
    private static final String NUMBER_ATTR = "number";
    private static final String BRANCH_ATTR = "branch";
    private static final String CONDITION_COVERAGE_ATTR = "condition-coverage";

    private static final String CONDITIONS_NODE = "conditions";

    private static final String CONDITION_NODE = "condition";
    private static final String COVERAGE_ATTR = "coverage";

    private static final CoverageFormatter formatter = new FloatFormatter();

    private XmlContext ctx;

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final String outputPath) throws IOException {
        this.ctx = new XmlContext(new FileOutputStream(outputPath));
    }

    private void startNode(final String name) {
        startNode(name,
                  new Runnable() {
                      @Override public void run() {}
                  });
    }

    private  <T> void startNode(final String name,
                                final Runnable attrs) {
        ctx.indent();
        ctx.print("<");
        ctx.print(name);
        attrs.run();
        ctx.println(">");
        ctx.incIndent();
    }

    private  <T> void singletonNode(final String name,
                                    final Runnable attrs) {
        ctx.indent();
        ctx.print("<");
        ctx.print(name);
        attrs.run();
        ctx.println("/>");
    }

    private void endNode(final String name) {
        ctx.decIndent();
        ctx.indent();
        ctx.print("</");
        ctx.print(name);
        ctx.println(">");
    }

    private void writeXMLHeader() {
        ctx.println(XML_HEADER);
        ctx.println(XML_DTD);
        ctx.println();
    }

    private void writeSource(final String srcRootPath) {
        ctx.indent();
        ctx.print("<");
        ctx.print(SOURCE_NODE);
        ctx.print(">");
        ctx.writeEscaped(srcRootPath);
        ctx.print("</");
        ctx.print(SOURCE_NODE);
        ctx.println(">");
    }

    private void writeSources(final Options options) {
        startNode(SOURCES_NODE);

        if (options.getSrcRootPaths() != null) {
            for (final String srcRootPath : options.getSrcRootPaths()) {
                writeSource(srcRootPath);
            }
        }

        endNode(SOURCES_NODE);
    }

    private void itemNodeAttrs(final ItemCoverage item) {
        ctx.attr(NUMBER_ATTR, item.getSourceLine());
        ctx.attr(HITS_ATTR, item.getCount());
        ctx.attr(BRANCH_ATTR, !item.isBlock());

        if (!item.isBlock()) {
            ctx.attr(CONDITION_COVERAGE_ATTR,
                     item.getCoverageString(DataType.BRANCH));
        }
    }

    private void writeItem(final ItemCoverage item) {
        singletonNode(LINE_NODE,
                      new Runnable() {
                          @Override
                          public void run() {
                              itemNodeAttrs(item);
                          }
                      });
    }

    private void writeItems(final Iterable<ItemCoverage> items) {
        startNode(LINES_NODE);

        for(final ItemCoverage item : items) {
            writeItem(item);
        }

        endNode(LINES_NODE);
    }

    private void methodNodeAttrs(final MethodCoverage method) {
        ctx.attr(NAME_ATTR, method.getName());
        ctx.attr(SIGNATURE_ATTR, method.getSignature());
        ctx.attr(LINE_RATE_ATTR, method.getCoverageString(DataType.LINE,
                                                          formatter));
        ctx.attr(BRANCH_RATE_ATTR, method.getCoverageString(DataType.BRANCH,
                                                            formatter));
        // See comments below
        ctx.attr(COMPLEXITY_ATTR, "1.0");
    }

    private void writeMethod(final MethodCoverage method) {
        startNode(METHOD_NODE,
                  new Runnable() {
                      @Override
                      public void run() {
                          methodNodeAttrs(method);
                      }
                  });

        writeItems(method);

        endNode(METHOD_NODE);
    }

    private void writeMethods(final Iterable<MethodCoverage> methods) {
        startNode(METHODS_NODE);

        for(final MethodCoverage method : methods) {
            writeMethod(method);
        }

        endNode(METHODS_NODE);
    }

    private void fieldNodeAttrs(final FieldCoverage field) {
        ctx.attr(NUMBER_ATTR, field.getStartLine());
        ctx.attr(HITS_ATTR, field.getHitCount());
        ctx.attr(BRANCH_ATTR, false);
    }

    private void writeField(final FieldCoverage field) {
        startNode(LINE_NODE,
                  new Runnable() {
                      @Override
                      public void run() {
                          fieldNodeAttrs(field);
                      }
                  });

        endNode(LINE_NODE);
    }

    /* Cobertura doesn't have fields, so we output these as line coverage. */
    private void writeClassLines(final ClassCoverage cls) {
        startNode(LINES_NODE);

        for(final MethodCoverage method : cls.getMethods()) {
            for(final ItemCoverage item : method) {
                writeItem(item);
            }
        }

        for(final FieldCoverage field : cls.getFields()) {
            writeField(field);
        }

        endNode(LINES_NODE);
    }

    private String trimPath(final String path,
                            final Options options) {
        if (options.getSrcRootPaths() != null) {
            for (final String srcRootPath : options.getSrcRootPaths()) {
                if (path.startsWith(srcRootPath)) {
                    int idx;

                    for (idx = srcRootPath.length();
                         path.charAt(idx) == File.separatorChar;
                         idx++);

                    return path.substring(idx);
                }
            }
        }

        return path;
    }

    private void classNodeAttrs(final ClassCoverage cls,
                                final Options options) {
        ctx.attr(NAME_ATTR, cls.getName());
        ctx.attr(FILENAME_ATTR, trimPath(cls.getSource(), options));
        ctx.attr(LINE_RATE_ATTR, cls.getCoverageString(DataType.LINE,
                                                       formatter));
        ctx.attr(BRANCH_RATE_ATTR, cls.getCoverageString(DataType.BRANCH,
                                                         formatter));
        // See comments below
        ctx.attr(COMPLEXITY_ATTR, "1.0");
    }

    private void writeClass(final ClassCoverage cls,
                            final Options options) {
        startNode(CLASS_NODE,
                  new Runnable() {
                      @Override
                      public void run() {
                          classNodeAttrs(cls, options);
                      }
                  });

        writeMethods(cls.getMethods());
        writeClassLines(cls);

        endNode(CLASS_NODE);
    }

    private void packageNodeAttrs(final PackageCoverage pack) {
        ctx.attr(NAME_ATTR, pack.getName());
        ctx.attr(LINE_RATE_ATTR, pack.getCoverageString(DataType.LINE,
                                                        formatter));
        ctx.attr(BRANCH_RATE_ATTR, pack.getCoverageString(DataType.BRANCH,
                                                          formatter));
        // It's not clear what this actually is supposed to mean, but
        // other tools just set it to 1.0
        ctx.attr(COMPLEXITY_ATTR, "1.0");
    }

    private void writeClasses(final Iterable<ClassCoverage> classes,
                              final Options options) {
        startNode(CLASSES_NODE);

        for(final ClassCoverage cls : classes) {
            writeClass(cls, options);
        }

        endNode(CLASSES_NODE);
    }

    private void writePackage(final PackageCoverage pack,
                              final Options options) {
        startNode(PACKAGE_NODE,
                  new Runnable() {
                      @Override
                      public void run() {
                          packageNodeAttrs(pack);
                      }
                  });
        writeClasses(pack, options);
        endNode(PACKAGE_NODE);
    }

    private void writePackages(final Iterable<PackageCoverage> packs,
                               final Options options) {
        startNode(PACKAGES_NODE);

        for(final PackageCoverage pack : packs) {
            writePackage(pack, options);
        }

        endNode(PACKAGES_NODE);
    }

    private void coverageNodeAttrs(final ProductCoverage coverage) {

        ctx.attr(LINES_COVERED_ATTR, coverage.getData(DataType.LINE).getCovered());
        ctx.attr(LINES_VALID_ATTR, coverage.getData(DataType.LINE).getTotal());
        ctx.attr(LINE_RATE_ATTR, coverage.getCoverageString(DataType.LINE,
                                                            formatter));
        ctx.attr(BRANCHES_COVERED_ATTR, coverage.getData(DataType.BRANCH).getCovered());
        ctx.attr(BRANCHES_VALID_ATTR, coverage.getData(DataType.BRANCH).getTotal());
        ctx.attr(BRANCH_RATE_ATTR, coverage.getCoverageString(DataType.BRANCH,
                                                              formatter));
        ctx.attr(TIMESTAMP_ATTR, Long.toString(System.currentTimeMillis() /
                                               1000L));

        // See comments below
        ctx.attr(COMPLEXITY_ATTR, "1.0");
        ctx.attr(VERSION_ATTR, "1.0");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void generateReport(final ProductCoverage coverage,
                               final Options options)
        throws IOException {
        writeXMLHeader();

        startNode(COVERAGE_NODE,
                  new Runnable() {
                      @Override
                      public void run() {
                          coverageNodeAttrs(coverage);
                      }
                  });

        writeSources(options);
        writePackages(coverage, options);

        endNode(COVERAGE_NODE);
        ctx.flush();
    }

    /**
     * Simple formatter that outputs a floating point number.  This is
     * used for Cobertura XML reports.
     */
    public static class FloatFormatter implements CoverageFormatter {
        @Override
        public String format(CoverageData data) {
            if (data.getTotal() != 0) {
                final double total = data.getTotal();
                final double covered = data.getCovered();

                return Double.toString(covered / total);
            } else {
                return "0.0";
            }
        }
    }
}
