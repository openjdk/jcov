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
package openjdk.codetools.jcov.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A file may have a set of &ldquo;items&rdquo, which are associated with the coverage collection. Examples:
 * methods, fields, try blocks, field usaget, etc, etc, etc. An &ldquo;items&rdquo may be related to one or more
 * portions of the source code, such as method is related to ranges of lines within the code, field - to
 * field declaration, etc.
 */
public interface FileItems {
    List<FileItem> items(String file);

    /**
     * Display name for the kind of the item. Probably plural: &ldquo;Methods&rdquo if the items represent methods
     */
    String kind();

    /**
     * Meanings for the colors. This is used in the reporting.
     */
    Map<Quality, String> legend();

    interface FileItem {
        /**
         * Display name for the item.
         */
        String item();

        /**
         * Ranges of the source code which this item is related to. Could be null.
         */
        List<LineRange> ranges();

        /**
         * How bad is the item?
         */
        Quality quality();
    }

    class FileItemImpl implements FileItem{

        private final String item;
        private final List<LineRange> ranges;
        private final Quality quality;

        public FileItemImpl(String item, List<LineRange> ranges, Quality quality) {
            this.item = item;
            this.ranges = ranges;
            this.quality = quality;
        }

        @Override
        public String item() {
            return item;
        }

        @Override
        public List<LineRange> ranges() {
            return ranges;
        }

        @Override
        public Quality quality() {
            return quality;
        }
    }
    /**
     * Items are grouped by the level of attention they require. Something bad deserves more attention and
     * likely to be red in the report.
     */
    enum Quality {
        VERY_GOOD, GOOD, SO_SO, BAD, IGNORE, NONE,
        LEFT, RIGHT, BOTH
    }

    class ItemsCache {
        private final FileItems items;
        private final FileSet files;
        private final Map<String, Map<Quality, Integer>> cache = new HashMap<>();

        public ItemsCache(FileItems items, FileSet files) {
            this.items = items;
            this.files = files;
        }

        //TODO will this work if the first call is with a file?
        public Map<Quality, Integer> count(String folder) {
            if (items == null) return Map.of();
            if (cache.containsKey(folder)) return cache.get(folder);
            var res = new HashMap<Quality, Integer>();
            items.legend().entrySet().stream().forEach(e -> res.put(e.getKey(), 0));
            files.files(folder).stream()
                    .forEach(f -> {
                        Map<Quality, Integer> colors;
                        if (cache.containsKey(f)) colors = cache.get(f);
                        else {
                            colors = new HashMap<>();
                            res.keySet().forEach(c ->
                            {
                                List<FileItem> items = this.items.items(f);
                                if (items != null)
                                    colors.put(c, (int) (items.stream()
                                        .filter(i -> i.quality() == c).count()));
                            });
                        }
                        colors.forEach((color, count) -> res.put(color, res.get(color) + count));
                        cache.put(f, colors);
                    });
            files.folders(folder).forEach(fld -> {
                count(fld).forEach((color, count) -> res.put(color, res.get(color) + count));
            });
            cache.put(folder, res);
            return res;
        }
    }
}
