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

import java.util.Collection;
import java.util.Objects;

<<<<<<< HEAD
import static java.lang.String.format;

=======
>>>>>>> 05fd4cae6a4651a07ecf85903355142573484a5a
/**
 * There is a fixed number of items of some sort. Some of those items can be covered.
 * @see CoveredLineRange
 */
public class Coverage {
    public static final Coverage COVERED = new Coverage(1,1);
    public static final Coverage UNCOVERED = new Coverage(0, 1);
    private final int covered;
    private final int total;
<<<<<<< HEAD
    private final FileItems.Quality quality;

    public Coverage(int covered, int total) {
        this(covered, total, covered > 0 ? FileItems.Quality.GOOD : FileItems.Quality.BAD);
    }

    public Coverage(int covered, int total, FileItems.Quality quality) {
        this.covered = covered;
        this.total = total;
        this.quality = quality;
    }

    public FileItems.Quality quality() {
        return quality;
=======

    public Coverage(int covered, int total) {
        this.covered = covered;
        this.total = total;
>>>>>>> 05fd4cae6a4651a07ecf85903355142573484a5a
    }

    public int covered() {
        return covered;
    }

    public int total() {
        return total;
    }

    public static Coverage sum(Collection<Coverage> coverages) {
        int covered = 0, total = 0;
        for (Coverage one : coverages) {
            covered += one.covered();
            total += one.total();
        }
        return new Coverage(covered, total);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coverage coverage = (Coverage) o;
        return covered == coverage.covered && total == coverage.total;
    }

    @Override
    public int hashCode() {
        return Objects.hash(covered, total);
    }

    @Override
    public String toString() {
<<<<<<< HEAD
        return format("%2.2f%%(%d/%d)", Math.ceil((double) covered/(double)total * 100), covered,  total);
=======
        return covered + "/" + total;
>>>>>>> 05fd4cae6a4651a07ecf85903355142573484a5a
    }
}
