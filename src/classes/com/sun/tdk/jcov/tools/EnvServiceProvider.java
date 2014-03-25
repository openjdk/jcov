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

import com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException;

/**
 *
 * @author Andrey Titov
 */
public interface EnvServiceProvider extends ServiceProvider {

    /**
     * <p> If a service should accept some specific CLI options - their
     * parameters should be added to EnvHandler as OptionDesc objects. </p> <p>
     * EnvHandler should not be overwritten - it's already existing handler.
     * Just add some new options with
     * <code>addOption(OptionDescr)</code> or
     * <code>addOptions(OptionDescr[])</code> methods </p>
     *
     * @param handler
     * @see EnvHandler
     * @see OptionDescr
     */
    public void extendEnvHandler(final EnvHandler handler);

    /**
     * <p> After CL string is parsed - JCov will call this method so that
     * Service can take some parameters from the environment. </p> <p> Use
     * <code>getValue(OptionDescr)</code>,
     * <code>getValues(OptionDescr)</code> and
     * <code>isSet(OptionDescr)</code> to get values of defined options. </p>
     *
     * @param handler
     * @return exit code. 0 means that everything was ok. Non-null return status
     * doesn't mean stopping the process but thrown Exception will stop the
     * process.
     */
    public int handleEnv(EnvHandler handler) throws EnvHandlingException;
}