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
package openjdk.jcov.data.fields.instrument;

import jdk.internal.classfile.Classfile;
import jdk.internal.classfile.FieldModel;
import openjdk.jcov.data.Env;
import openjdk.jcov.data.fields.runtime.Collect;
import openjdk.jcov.data.fields.runtime.Runtime;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static openjdk.jcov.data.Env.JCOV_DATA_ENV_PREFIX;

public class TemplateGenerator {
    public static final String FIELD_FILTER =
            JCOV_DATA_ENV_PREFIX + openjdk.jcov.data.arguments.runtime.Collect.ARGUMENTS_PREFIX + "field.filter";

    public interface FieldFilter {
        boolean accept(FieldModel fieldModel) throws Exception;
    }

    private FieldFilter fieldFilter;

    public TemplateGenerator() throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        this.fieldFilter = Env.getSPIEnv(FIELD_FILTER, fm -> true);
    }

    public void setFieldFilter(FieldFilter fieldFilter) {
        this.fieldFilter = fieldFilter;
    }

    public void generate(Path root) throws IOException {
        List<Path> allClasses = new ArrayList<>();
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().endsWith(".class")) {
                    allClasses.add(file);
                }
                return super.visitFile(file, attrs);
            }
        });
        generate(allClasses);
    }

    public void generate(List<Path> classes) {
        classes.forEach(file -> {
            try {
                Classfile.parse(Files.readAllBytes(file)).fields().forEach(fm -> {
                    try {
                        if (fieldFilter.accept(fm)) Collect.template(fm);
                    } catch (Exception e) {
                        throw new RuntimeException();
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static void main(String[] args) throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Runtime.init();
        TemplateGenerator generator = new TemplateGenerator();
        if (args.length > 1) {
            generator.generate(Arrays.stream(args).map(Path::of).collect(Collectors.toList()));
        } else if (args[0].startsWith("jrt:/")) {
            generator.generate(FileSystems.getFileSystem(URI.create("jrt:/")).getPath(args[0].substring("jrt:/".length())));
        } else {
            generator.generate(Path.of(args[0]));
        }
    }
}
