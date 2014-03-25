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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p> SPIDescr is an object-descriptor for handling Service Providers in
 * EnvHandler </p>
 *
 * @author Andrey Titov
 */
public class SPIDescr {

    private String name;
    private String description;
    private boolean isMultiple;
    private String usage;
    private Map<String, ServiceProvider> presets;
    private boolean handled = false;
    private Class<? extends ServiceProvider> spiClass;
    private ServiceProvider defaultSPI;

    /**
     * Creates instance of SPIDescr class representing SPI
     *
     * @param name Name to use as alias in option
     * @param spiClass Underlying ServiceProvider class
     */
    public SPIDescr(String name, Class<? extends ServiceProvider> spiClass) {
        this.name = name;
        this.spiClass = spiClass;
    }

    /**
     * Creates instance of SPIDescr class representing SPI
     *
     * @param name Name to use as alias in option
     * @param spiClass Underlying ServiceProvider class
     * @param description Description for this ServiceProvider to print in the
     * help messages
     */
    public SPIDescr(String name, Class<? extends ServiceProvider> spiClass, String description) {
        this.name = name;
        this.description = description;
        this.spiClass = spiClass;
    }

    /**
     * Creates instance of SPIDescr class representing SPI
     *
     * @param name Name to use as alias in option
     * @param spiClass Underlying ServiceProvider class
     * @param description Description for this ServiceProvider to print in the
     * help messages
     * @param isMultiple Whether this SPI can be registered several times (NYI)
     * @param usage Usage string to print in the help messages
     */
    public SPIDescr(String name, Class<? extends ServiceProvider> spiClass, String description, boolean isMultiple, String usage) {
        this.name = name;
        this.description = description;
        this.isMultiple = isMultiple;
        this.usage = usage;
        this.spiClass = spiClass;
    }

    /**
     * <p> Registers a shortcut for the SPI. For example you can register
     * shortcut "none" for empty Service and it will be available through the
     * options: "-my.spi none". In this case EnvHandler will not try to load a
     * class "none" but will find preset for it. </p> <p> Note that if
     * EnvServiceProvider will be used - EnvHandler will call defineHandler. You
     * don't have to call it manually. </p>
     *
     * @param preset Preset name. E.g. "none", "all", ...
     * @param defaultSPI ServiceProvider instance to use for the preset
     * @throws IllegalStateException when EnvHandler already did the
     * initialization
     */
    public void addPreset(String preset, ServiceProvider defaultSPI) {
        if (handled) {
            throw new IllegalStateException("Can't modify SPI after handling the SPI");
        }
        if (defaultSPI == null) {
            throw new IllegalArgumentException("Alias for a Service Provider can't be null. Use setDefault instead. ");
        }
        if (!spiClass.isInstance(defaultSPI)) {
            throw new IllegalArgumentException("Illegal default Service Provider class. Found " + defaultSPI.getClass() + ", required " + spiClass);
        }
        if (presets == null) {
            presets = new HashMap<String, ServiceProvider>();
        }
        presets.put(preset, defaultSPI);
    }

    void setHandled(boolean handled) {
        this.handled = handled;
    }

    /**
     * Checks whether it's the name of this SPI
     *
     * @param name Name to check
     * @return true or false
     */
    public boolean isName(String name) {
        if (name != null) {
            return name.equals(this.name);
        }
        return this.name == null;
    }

    /**
     *
     * @return Underlying ServiceProvider class
     */
    public Class<? extends ServiceProvider> getSPIClass() {
        return spiClass;
    }

    /**
     * <p> Get SPI name which is used to set the SPI implementation through
     * environment </p>
     *
     * @return SPI name
     */
    public String getName() {
        return name;
    }

    /**
     * <p> Get all registered SPI presets. See
     * {@link SPIDescr#addPreset(java.lang.String, com.sun.tdk.jcov.tools.ServiceProvider)}
     * for more details. </p>
     *
     * @return SPI presets or null if no presets were added
     */
    public Collection<ServiceProvider> getPresets() {
        if (presets == null) {
            return null;
        }
        return presets.values();
    }

    /**
     * <p> Get SPI preset ServiceProvider. See
     * {@link SPIDescr#addPreset(java.lang.String, com.sun.tdk.jcov.tools.ServiceProvider)}
     * for more details. </p>
     *
     * @param className
     * @return ServiceProvider instance registered for this preset
     */
    public ServiceProvider getPreset(String className) {
        if (presets == null) {
            return null;
        }
        return presets.get(className);
    }

    /**
     * <p> Get all presets for the SPI. See
     * {@link SPIDescr#addPreset(java.lang.String, com.sun.tdk.jcov.tools.ServiceProvider)}
     * for more details. </p>
     *
     * @return Presets for the SPI.
     */
    public Map<String, ServiceProvider> getPresetsMap() {
        return presets;
    }

    /**
     * <p> Set the default ServiceProvider for this SPI. It will be used if no
     * SPI will be specified by the user. </p>
     *
     * @param defaultSPI Default ServiceProvider
     */
    public void setDefaultSPI(ServiceProvider defaultSPI) {
        this.defaultSPI = defaultSPI;
    }

    /**
     * <p> Get the default ServiceProvider. Can be null. </p>
     *
     * @return Default ServiceProvider
     */
    public ServiceProvider getDefaultSPI() {
        return defaultSPI;
    }
}