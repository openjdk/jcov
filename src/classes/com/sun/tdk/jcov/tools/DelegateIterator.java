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
package com.sun.tdk.jcov.tools;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 * @author Sergey Borodin
 */
public abstract class DelegateIterator<E> implements Iterator<E> {

    public DelegateIterator() {
        mode = 0;
        delegate = nextIterator();
    }
    /**
     * 0 - "In progress" state 1 - terminate state
     */
    private int mode;
    /**
     * current delegating iterator null means there is nothing more to iterate
     */
    private Iterator<E> delegate;

    public boolean hasNext() {
        if (mode == 1) {
            return false;
        } else {
            if (delegate == null) {
                mode = 1;
                return false;
            } else {
                if (!delegate.hasNext()) {
                    delegate = nextIterator();
                    return hasNext();
                } else {
                    return true;
                }
            }
        }
    }

    public E next() {
        if (mode == 1) {
            throw new NoSuchElementException();
        } else if (delegate == null) {
            mode = 1;
            throw new NoSuchElementException();
        } else {
            if (!delegate.hasNext()) {
                delegate = nextIterator();
                return next();
            } else {
                return delegate.next();
            }
        }
    }

    protected abstract Iterator<E> nextIterator();

    public void remove() {
    }
}
