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
package openjdk.jcov.data.runtime;

import openjdk.jcov.data.Env;
import openjdk.jcov.data.arguments.runtime.Collect;
import openjdk.jcov.data.runtime.serialization.ToStringSerializer;

import java.util.function.Function;

public interface Serializer extends Function<Object, String> {
    Function<Object, String> TO_STRING = new ToStringSerializer();

    /**
     * Name of a property containing a class name of a class of type <code>Function<Object, String></code> which will
     * be used during the serialization. <code>Object::toString</code> is used by default.
     */
    String SERIALIZER = Env.JCOV_DATA_ENV_PREFIX +
            Collect.ARGUMENTS_PREFIX + "serializer";

}
