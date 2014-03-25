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
package com.sun.tdk.jcov.processing;

/**
 *
 * @author Dmitry Fazunenko
 */
public class DataProcessorFactory {

    /**
     * Default SPI to be used.
     */
    public static final String DEFAULT_SPI =
            //        "com.sun.tdk.jcov.processing.fx.FXDataProcessorFactorySpi";
            "com.sun.tdk.jcov.processing.DefaultDataProcessorSpi";
    /**
     * Property name to specify DataProcessorFactorySpi class name
     */
    public static final String PROP_NAME = "dataprocessor.spi";
    private DataProcessorSPI spi = null;

    private DataProcessorFactory(String spiClassName) {
        if (spiClassName == null) {
            spi = null;
        } else {
            try {
                Class spiClass = Class.forName(spiClassName);
                spi = (DataProcessorSPI) spiClass.newInstance();
            } catch (Exception e) {
                throw new Error("Cannot create an instance of "
                        + " DataProcessorFactorySpi: " + e);
            }
        }
    }

    /**
     * Returns DataProcessorFactory instance. DataProcessorFactory uses
     * DataProcessorFactorySpi to get class member filter which is initialized
     * during the first call of this method. Name of SPI class is taken from the
     * <code>dataprocessor.spi</code> system property. If the property is not
     * set, the default value will be used instead.
     *
     * @return factory instance
     * @throws Error if instance of DataProcessorFactory cannot be created.
     *
     */
    public static DataProcessorFactory getInstance() {
        String spiClassName = System.getProperty(PROP_NAME);
        if (spiClassName == null) {
            spiClassName = DEFAULT_SPI;
        } else if ("none".equalsIgnoreCase(spiClassName)) {
            spiClassName = StubSpi.class.getName();
        }
        return new DataProcessorFactory(spiClassName);
    }

    /**
     * Returns data processor. Never returns null.
     */
    public DataProcessor getDataProcessor() {
        return spi == null ? DataProcessor.STUB : spi.getDataProcessor();
    }
}
