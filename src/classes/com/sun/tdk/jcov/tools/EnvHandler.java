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

import com.sun.tdk.jcov.runtime.PropertyFinder;
import com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * <p> This class provides easy way to parse command line options, retrieve/set
 * options values. Each option is assumed to have the following syntax :
 * -&lt;option_name&gt; [&lt;option_value&gt;] and can be specified more than
 * once. If an option is specified multiple times, than all its values can be
 * retrieved by the getValues() method. </p>
 *
 * @author Konstantin Bobrovsky
 */
public class EnvHandler {

    public final static String sccsVersion = "1.6 01/17/03";
    public static final String OPTION_SPECIFIER = "-";
    public static final String OPT_VAL_DELIM = "=";
    public static final String PROP_FILE_SPECIFIER = "@";
    public static final String PROP_OPT_DELIM = ";";
    public static final String AGENT_OPT_DELIM = ",";
    /**
     * Non-options arguments
     */
    private String[] tail;
    /**
     * repository for (option, value) pairs
     */
    protected Map<String, List<String>> options = new HashMap<String, List<String>>();
    /**
     * descriptions of options which will be accepted by the parse method
     */
    protected List<OptionDescr> validOptions;
    /**
     * Owner of this handler (used for printing usage)
     */
    protected JCovTool tool;
    /**
     * Properties read from CLI (
     *
     * @ support)
     */
    Properties CLProperties = new Properties();
    private List<SPIDescr> spiDescrs = new LinkedList<SPIDescr>();
    private Map<SPIDescr, ArrayList<ServiceProvider>> readProviders = new HashMap<SPIDescr, ArrayList<ServiceProvider>>();
    private ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    private EnvServiceProvider extendingSPI;

    /**
     * checks if given string can be an option
     */
    public static boolean isOption(String s) {
        return s.startsWith(OPTION_SPECIFIER);
    }

    @Deprecated
    /**
     * EnvHandler should be constructed with JCovTool instance
     */
    public EnvHandler() {
        validOptions = new LinkedList<OptionDescr>();
        for (OptionDescr d : SHARED_OPTIONS) {
            validOptions.add(d);
        }
    }

    public EnvHandler(OptionDescr[] valid_options, JCovTool tool) {
        // validOptions = new ArrayList<OptionDescr>(Arrays.asList(valid_options));
        // no need to add elements so just using Arrays wrapper
        validOptions = new LinkedList(Arrays.asList(valid_options));
        for (OptionDescr d : SHARED_OPTIONS) {
            validOptions.add(d);
        }
        this.tool = tool;
    }

    public EnvHandler(List<OptionDescr> validOptions, JCovTool tool) {
        this.validOptions = validOptions;
        for (OptionDescr d : SHARED_OPTIONS) {
            validOptions.add(d);
        }
        this.tool = tool;
    }

    private OptionDescr getOptionByName(String name) {
        for (int i = 0; i < validOptions.size(); i++) {
            if (validOptions.get(i).isName(name)) {
                return validOptions.get(i);
            }
        }
        return null;
    }

    /**
     * <p> Finds SPIDescr by name assigned to the SPI. SPI name is a string
     * which can be used by a user to assign custom ServiceProvider through CLI,
     * variables, properties and so on. </p>
     *
     * @param name SPIDescr name
     * @return
     */
    public SPIDescr getSPIDescrByName(String name) {
        for (SPIDescr spi : spiDescrs) {
            if (spi.isName(name)) {
                return spi;
            }
        }
        return null;
    }

    /**
     * <p> Finds SPIDescr by underlying ServiceProvider class </p>
     *
     * @param spiClass ServiceProviced class to find
     * @return SPIDescr assigned for the ServiceProvider. <code>null</code> if
     * the ServiceProvider is not registered.
     */
    public SPIDescr getSPIByClass(Class<? extends ServiceProvider> spiClass) {
        for (SPIDescr spi : spiDescrs) {
            if (spiClass == spi.getSPIClass()) {
                return spi;
            }
        }
        return null;
    }

    private void addSPI(SPIDescr spi, ServiceProvider customProviderInstance) {
        if (customProviderInstance instanceof EnvServiceProvider) {
            extendingSPI = (EnvServiceProvider) customProviderInstance;
            extendingSPI.extendEnvHandler(this);
            extendingSPI = null;
        }
        ArrayList<ServiceProvider> list = readProviders.get(spi);
        if (list == null) {
            list = new ArrayList<ServiceProvider>(1);
            list.add(customProviderInstance);
            readProviders.put(spi, list);
        } else {
            list.add(customProviderInstance);
        }
    }

    /**
     * <p> Load and pre-init (call defineHandler()) a ServiceProvider. </p> <p>
     * Note that this ServiceProvider will be registered in the EnvHandler
     * automatically as <b>spiClass</b>. Pass null to <b>spiClass</b> to disable
     * registering. </p>
     *
     * @param spiClass
     * @param className
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    private ServiceProvider instantiateServiceProvider(SPIDescr spi, String className) throws InstantiationException, IllegalAccessException, ClassNotFoundException, Exception {
        ServiceProvider alias = spi.getPreset(className);
        if (alias != null) {
            addSPI(spi, alias);
            return alias;
        }

        //Class<? extends ServiceProvider> customProviderClass = (Class<? extends ServiceProvider>) classLoader.loadClass(className);
        ServiceProvider customProviderInstance = (ServiceProvider) classLoader.loadClass(className).newInstance();

        addSPI(spi, customProviderInstance);
        return customProviderInstance;
    }

    /**
     * Parse command line arguments and fill environment
     *
     * @param options_array command line (or parsed agent line) params
     * @throws Exception
     */
    public void parseCLIArgs(String options_array[]) throws CLParsingException {
        ArrayList<String> opts = new ArrayList<String>(Arrays.asList(options_array));
        LinkedList<String> tailList = new LinkedList<String>();
        CLProperties = new Properties();

        processSPIs(opts);

        // we want to get all real options in any case to be able to check -h
        CLParsingException toThrow = null;
        try {
            ListIterator<String> it = opts.listIterator();
            // Looking for -propfile option specified
            while (it.hasNext()) {
                String opt = it.next();
                if (isOption(opt)) {
                    opt = opt.substring(1);
                    if (PROPERTYFILE.isName(opt)) {
                        if (!it.hasNext()) {
                            if (toThrow == null) {
                                toThrow = new CLParsingException("Option '" + opt + "' is invalid: value expected.");
                            }
                        }
                        String prop = it.next();
                        if ("".equals(prop)) {
                            break;
                        }
                        PropertyFinder.setPropertiesFile(prop);
                        break;
                    }
                }
            }

            it = opts.listIterator();
            while (it.hasNext()) {
                String option = it.next();
                if (option.startsWith(PROP_FILE_SPECIFIER)) {
                    String filename = option.substring(1);
                    CLProperties = PropertyFinder.readProperties(filename, CLProperties);
                } else {
                    if (!isOption(option)) {
                        tailList.add(option); // not an option and not an option value - error or tail
                    } else {
                        option = option.substring(1); // removing OPTION_SPECIFIER
                        OptionDescr optDescr = getOptionByName(option);
                        // no need to check allowedOptions - getOptionByName will return null in case such option is invalid (HELP and HELP_VERBOSE are allowed for all)
                        if (optDescr == null) {
                            if (toThrow == null) {
                                toThrow = new CLParsingException("Option '" + option + "' is invalid.");
                            }
                        } else {
                            if (!optDescr.isMultiple && options.containsKey(optDescr.name)) {
                                if (toThrow == null) {
                                    toThrow = new CLParsingException("Option is specified twice : " + option);
                                }
                            }

                            if (optDescr.hasValue) {
                                try {
                                    processOption(it, optDescr, option);
                                } catch (NoSuchElementException e) { // it.next()
                                    if (toThrow == null) {
                                        toThrow = new CLParsingException("Option '" + option + "' is invalid: value expected.");
                                    }
                                }
                            } else {
                                setOption(optDescr);
                            }
                        }
                    }
                }
            }
        } finally {
            if (toThrow != null) {
                throw toThrow;
            }
        }

        if (!tailList.isEmpty()) {
            tail = tailList.toArray(new String[tailList.size()]);
        } else {
            tail = null;
        }
    }

    private void processOption(ListIterator<String> it, OptionDescr optDescr, String option) throws CLParsingException {
        String value;
        if (optDescr.val_kind == OptionDescr.VAL_ALL) {
            if (!it.hasNext()) {
                throw new NoSuchElementException();
            }
            while (it.hasNext()) {
                value = it.next();
                addValuedOption(value, option, optDescr);
            }
        } else {
            value = it.next();

            if (value.startsWith(PROP_FILE_SPECIFIER)) {
                value = PropertyFinder.readPropFrom(value.substring(1), option);
            }

            addValuedOption(value, option, optDescr);
        }
    }

    private void processSPIs(ArrayList<String> opts) throws CLParsingException {
        if (!spiDescrs.isEmpty()) {
            // Loading registered SPIs
            ListIterator<String> it = opts.listIterator();
            while (it.hasNext()) {
                String opt = it.next();
                if (isOption(opt)) {
                    opt = opt.substring(1); // cutting "-"
                    SPIDescr descr = getSPIDescrByName(opt);
                    if (descr != null) {
                        if (!it.hasNext()) {
                            throw new CLParsingException("Service provider '" + opt + "' is invalid: value expected");
                        }
                        it.remove();
                        String className = it.next();
                        it.remove();
                        try {
                            instantiateServiceProvider(descr, className);
                        } catch (ClassNotFoundException e) {
                            throw new CLParsingException("Service Provider class '" + className + "' not found");
                        } catch (ClassCastException e) {
                            throw new CLParsingException("Invalid class for Service Provider '" + opt + "': '" + className + "' - not a Service provider");
                        } catch (InstantiationException e) {
                            throw new CLParsingException("Can't instantiate Service Provider class '" + className + "'");
                        } catch (Exception e) {
                            throw new CLParsingException("Error loading Service Provider '" + className + "': exception " + e.getClass() + " occured '" + e.getMessage() + "'", e);
                        }
                    }
                }
            }

            // Initializing loaded SPIs
            for (SPIDescr descr : spiDescrs) {
                if (!readProviders.containsKey(descr)) {
                    String className = PropertyFinder.findValue(descr.getName(), null);
                    if (className != null) {
                        try {
                            instantiateServiceProvider(descr, className);
                        } catch (ClassNotFoundException e) {
                            throw new CLParsingException("Service Provider class '" + className + "' not found");
                        } catch (ClassCastException e) {
                            throw new CLParsingException("Invalid class for Service Provider '" + descr.getName() + "': '" + className + "' - not a Service provider");
                        } catch (InstantiationException e) {
                            throw new CLParsingException("Can't instantiate Service Provider class '" + className + "'");
                        } catch (Exception e) {
                            throw new CLParsingException("Error loading Service Provider '" + className + "': exception " + e.getClass() + " occured '" + e.getMessage() + "'", e);
                        }
                    } else {
                        if (descr.getDefaultSPI() != null) {
                            addSPI(descr, descr.getDefaultSPI());
                        }
                    }
                }
            }
        }
    }

    /**
     * Check whether option name is registered in this envhandler. Uses
     * getOptionByName
     *
     * @param name option name
     * @return true if name is in validOptions of envhandler or if it's system
     * option (HELP, HELP_VERBOSE)
     * @see #getOptionByName(java.lang.String)
     */
    public boolean isValidOption(String name) {
        return getOptionByName(name) == null;
    }

    private void setOption(OptionDescr optDescr) {
        options.put(optDescr.name, null);
    }

    private void addValuedOption(String value, String option, OptionDescr optDescr) throws CLParsingException {
        if (value == null) {
            throw new CLParsingException("Option '" + option + "' is invalid: value expected.");
        }

        if (!optDescr.isAllowedValue(value)) { // if allowedValues are null - returns true
            throw new CLParsingException("Invalid value for option '" + option + "' : '" + value + "'");
        }

        if (!optDescr.isMultiple) {
            ArrayList<String> list = new ArrayList<String>(1);
            list.add(value);
            options.put(optDescr.name, list);
        } else {
            List<String> list = options.get(optDescr.name);
            if (list == null) {
                list = new LinkedList<String>();
                options.put(optDescr.name, list);
            }
            list.add(value);
        }
    }

    /**
     * Parses agent parameters string (option=value,option,option=value) to
     * string array
     *
     * @param args arguments to parse (option=value,option,option=value)
     * @return string array (option; value; option; option; value)
     */
    public static String[] parseAgentString(String args) {
        if (args == null || args.trim().length() == 0) {
            return new String[0];
        }

        String[] opts = args.split(AGENT_OPT_DELIM);
        LinkedList<String> res = new LinkedList<String>();
        for (String s : opts) {
            int ind = s.indexOf(OPT_VAL_DELIM);
            if (ind == -1) {
                if (s.contains(PROP_FILE_SPECIFIER)) {
                    res.add(s);
                } else {
                    res.add(OPTION_SPECIFIER + s);
                }
            } else {
                res.add(OPTION_SPECIFIER + s.substring(0, ind));
                res.add(s.substring(ind + 1));
            }
        }
        return res.toArray(new String[res.size()]);
    }

    /**
     * sets the option with the specified name. The effect is the same as of the
     * parse("-&lt;option_name&gt;") invocation.
     */
    public void set(String option_name) {
        set(option_name, (String) null);
    }

    /**
     * assigns the specified value to the option. Any previously assigned values
     * are destroyed.
     */
    public void set(String option_name, String option_value) {
        List<String> opt_values = options.get(option_name);
        if (opt_values == null) {
            opt_values = new LinkedList<String>();
            options.put(option_name, opt_values);
        }
        if (option_value == null) {
            return;
        }
        opt_values.add(option_value);
    }

    /**
     * assigns the specified array of values to the option. Any previously
     * assigned values are destroyed.
     */
    public void set(String option_name, String[] option_values) {
        if (option_values == null) {
            options.put(option_name, null);
        } else {
            List<String> opt_values = new ArrayList<String>(Arrays.asList(option_values));
            options.put(option_name, opt_values);
        }
    }

    /**
     * assigns the specified vector of values to the option. Any previously
     * assigned values are destroyed.
     */
    public void set(String option_name, List<String> option_values) {
        options.put(option_name, option_values);
    }

    public String getCleanValue(OptionDescr option) {
        if (!validOptions.contains(option)) {
            return null;
        }
        if (!option.hasValue && isSet(option)) {
            return "on";
        }
        String[] values = getCleanValues(option);
        if (values == null) {
            return option.defVal;
        }
        return values[0];
    }

    public String getValue(OptionDescr option) {
        if (!validOptions.contains(option)) {
            return option.defVal;
        }
        if (!option.hasValue && isSet(option)) {
            return "on";
        }
        String[] values = getValues(option);
        if (values == null) {
            return PropertyFinder.processMacroString(option.defVal, null, null);
        }
        return values[0];
    }

    /**
     * returns all values of given option. For example, for "-opt=val0
     * -opt=val1" options getValues("opt") will return {"val0", "val1"}.
     */
    private String[] getValues(String option_name) {
        List<String> opt_values = options.get(option_name);
        if (opt_values == null) { // no such option in the CL environment
            if (CLProperties.containsKey("jcov." + option_name)) {
                return ((String) CLProperties.get("jcov." + option_name)).split(PROP_OPT_DELIM);
            }

            String res = PropertyFinder.findValue(option_name, null);
            if (res != null) {
                return res.split(PROP_OPT_DELIM);
            }

            return null;
        }
        if (opt_values.isEmpty()) {
            return null;
        }
        return opt_values.toArray(new String[opt_values.size()]);
    }

    public String[] getCleanValues(OptionDescr option) {
        if (!validOptions.contains(option)) {
            return null;
        }
        if (!option.hasValue && isSet(option)) {
            return new String[]{"on"};
        }
        String[] values = getValues(option.name);
        if (values == null || values.length == 0) {
            return option.defVal == null ? null : new String[]{option.defVal};
        }
        return values;
    }

    /**
     * returns all values of given option. For example, for "-opt=val0
     * -opt=val1" options getValues("opt") will return {"val0", "val1"}.
     */
    public String[] getValues(OptionDescr option) {
        String[] values = getCleanValues(option);
        if (values != null) {
            for (int i = 0; i < values.length; ++i) {
                values[i] = PropertyFinder.processMacroString(values[i], null, null);
            }
        }
        return values;
    }

    /**
     * Returns tail, the least of parameters with put options at the end of
     * args.
     */
    public String[] getTail() {
        return tail;
    }

    /**
     * checks if given options has been successfully parsed by the parse(String)
     * method
     */
    public boolean isSet(OptionDescr option) {
        if (!validOptions.contains(option)) {
            return false;
        }
        return options.containsKey(option.name) || CLProperties.containsKey("jcov." + option.name) || (PropertyFinder.findValue(option.name, null) != null);
    }

    /**
     * @return String representation of currently set options, as they would
     * appear on the command line
     */
    public String unParse() {
        StringBuilder builder = new StringBuilder();
        Iterator<String> it = options.keySet().iterator();
        while (it.hasNext()) {
            String opt_name = it.next();
            String[] opt_values = getValues(opt_name);
            if (opt_values != null) {
                for (int j = 0; j < opt_values.length; j++) {
                    builder.append(OPTION_SPECIFIER).append(opt_name).append(OPT_VAL_DELIM).append(opt_values[j]);
                    if (j < opt_values.length - 1) {
                        builder.append(" ");
                    }
                }
            } else {
                builder.append(OPTION_SPECIFIER).append(opt_name);
            }
            if (it.hasNext()) {
                builder.append(" ");
            }
        }
        return builder.toString();
    }

    public void printEnv() {
        for (OptionDescr descr : validOptions) {

            String source = null;
            if (options.containsKey(descr.name)) {
                source = "cmd";
            } else {
                source = PropertyFinder.findSource(descr.name);
            }

            if (descr.equals(PRINT_ENV) && "defaults".equals(source)) {
                continue;
            }

            if (descr.isMultiple) {
                System.out.println("Property '" + descr.name + "' has values '" + Arrays.toString(getValues(descr)) + "' from " + source);
            } else {
                if (descr.hasValue) {
                    System.out.println("Property '" + descr.name + "' has value '" + getValue(descr) + "' from " + source);
                } else {
                    if (isSet(descr)) {
                        System.out.println("Property '" + descr.name + "' is set from " + source);
                    }
                }
            }
        }
    }

    public Map<String, List<String>> getFullEnvironment() {
        Map<String, List<String>> map = new HashMap<String, List<String>>(options);
        for (OptionDescr opt : validOptions) {
            if (map.containsKey(opt.name)) {
                continue;
            }

            if (!opt.hasValue) {
                map.put(opt.name, null);
                continue;
            }

            String[] values = getValues(opt);
            map.put(opt.name, Arrays.asList(values));
        }
        return map;
    }
    /**
     * @return the number of options set
     */
    public int size() {
        return options.size();
    }

    public void usage(boolean verbose) {
        if (tool == null) {
            return;
        }
        if (!verbose) {
            out.println("Try \"-help-verbose\" for more detailed help");
        }
        out.println("Usage:");
        out.println("> " + tool.usageString());
        if (validOptions != null) {
            out.println("    options:");
            for (OptionDescr od : validOptions) {
                if (SHARED_OPTIONS.contains(od)) {
                    continue;
                }
                out.println(od.getUsage("-", " ", "    ", verbose));
            }
            for (OptionDescr od : SHARED_OPTIONS) {
                out.println(od.getUsage("-", " ", "    ", verbose));
            }
        }

        if (!spiDescrs.isEmpty()) {
            out.println("Service providers: ");
            for (SPIDescr spi : spiDescrs) {
                out.print("    -");
                out.print(spi.getName());
                out.print(" : ");
                Class clazz = spi.getSPIClass();
                out.println(clazz.getSimpleName());
            }
        }
        out.print("Example: ");
        out.println(tool.exampleString());

    }

    public void usage() {
        usage(isSet(HELP_VERBOSE));
    }
    private PrintStream out = System.out;

    public void setOut(PrintStream out) {
        this.out = out;
    }

    public PrintStream getOut() {
        return out;
    }

    /**
     * <p> Registers new SPI to be processed by JCov </p> <p> Registering SPI
     * should be done before EnvHandler will process environment - in
     * Tool.defineEnv() method or before Tool.handleEnv() method. </p>
     *
     * @param spiDescr SPI to register
     * @see EnvHandler#getSPIs(com.sun.tdk.jcov.tools.SPIDescr)
     */
    public void registerSPI(SPIDescr spiDescr) {
        if (spiDescrs.contains(spiDescr)) {
            return;
        }
        spiDescrs.add(spiDescr);
        Collection<ServiceProvider> defs = spiDescr.getPresets();
        if (defs != null) {
            for (ServiceProvider def : defs) {
                if (def instanceof EnvServiceProvider) {
                    extendingSPI = (EnvServiceProvider) def;
                    extendingSPI.extendEnvHandler(this);
                    extendingSPI = null;
                }
            }
        }
    }

    /**
     * <p> Removes option from JCov environment so that it wouldn't be
     * accessible through getValue() method </p>
     *
     * @param descr Option to remove
     * @return true if this option was removed
     */
    public boolean removeOption(OptionDescr descr) {
        return validOptions.remove(descr);
    }

    /**
     * Adds OptionsDescr to this environment so that it will be parsed by
     * EnvHandler
     *
     * @param descr OptionDescr to add
     * @return true when OptionDescr was successfully added
     */
    public boolean addOption(OptionDescr descr) {
        // check that such option doesn't exitst
        int pos = 1, foundPos = -1;
        for (OptionDescr d : validOptions) {
            if (d.name.equals(descr.name)) {
                return false;
            }
            if (foundPos < 0) {
                if (d.title != null && !d.title.isEmpty() && d.title.equals(descr.title)) {
                    descr.title = "";
                    foundPos = pos;
                }
            }
            ++pos;
        }
        if (extendingSPI != null) {
            descr.setRegisteringSPI(extendingSPI);
        }
        if (foundPos > 0) {
            validOptions.add(foundPos, descr);
        } else {
            validOptions.add(descr);
        }
        return true;
    }

    /**
     * Adds several options to JCov environment
     *
     * @param optionDescr Options to add
     */
    public void addOptions(OptionDescr[] optionDescr) {
        for (OptionDescr opt : optionDescr) {
            addOption(opt);
        }
    }

    /**
     * <p> Get all instantiated ServiceProviders for registered ServiceProvider
     * (SPIClass) </p>
     *
     * @param <SPIClass> Registered ServiceProvider class
     * @param spiClass Registered ServiceProvider class
     * @return List of all instantiated Providers for the class
     */
    public <SPIClass extends ServiceProvider> ArrayList<SPIClass> getSPIs(Class<SPIClass> spiClass) {
        return (ArrayList<SPIClass>) getSPIs(getSPIByClass(spiClass));
    }

    /**
     * <p> Get all instantiated ServiceProviders for registered ServiceProvider
     * </p>
     *
     * @param descr Registered SPI
     * @return List of all instantiated Providers for the SPI
     */
    public ArrayList<ServiceProvider> getSPIs(SPIDescr descr) {
        if (descr == null) {
            return new ArrayList<ServiceProvider>(0);
        }

        ArrayList<ServiceProvider> get = readProviders.get(descr);
        if (get == null) {
            if (descr.getDefaultSPI() != null) {
                get = new ArrayList<ServiceProvider>(1);
                get.add(descr.getDefaultSPI());
                return get;
            }
            return new ArrayList<ServiceProvider>(0);
        }
        return (ArrayList<ServiceProvider>) get;
    }

    /**
     * <p> Initializes all instantiated ServiceProviders by calling handleEnv()
     * for each of them. </p> <p> This method will not stop if any of the
     * ServiceProviders will return not-null exit code but will stop in case of
     * any exception </p>
     *
     * @return Initialization exit code (0)
     * @throws com.sun.tdk.jcov.tools.JCovTool.EnvHandlingException
     */
    public int initializeSPIs() throws EnvHandlingException {
        for (ArrayList<ServiceProvider> list : readProviders.values()) {
            for (ServiceProvider sp : list) {
                if (sp instanceof EnvServiceProvider) {
                    int retCode = ((EnvServiceProvider) sp).handleEnv(this);
                }
            }
        }
        for (SPIDescr spi : spiDescrs) {
            if (spi.getDefaultSPI() != null && (spi.getDefaultSPI() instanceof EnvServiceProvider)) {
                ((EnvServiceProvider) spi.getDefaultSPI()).handleEnv(this);
            }
            Collection<ServiceProvider> aliaseSPIs = spi.getPresets();
            if (aliaseSPIs != null && !aliaseSPIs.isEmpty()) {
                for (ServiceProvider sp : aliaseSPIs) {
                    if (sp instanceof EnvServiceProvider) {
                        ((EnvServiceProvider) sp).handleEnv(this);
                    }
                }
            }
        }
        return 0;
    }

    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static class CLParsingException extends Exception {

        public CLParsingException(Throwable cause) {
            super(cause);
        }

        public CLParsingException(String message, Throwable cause) {
            super(message, cause);
        }

        public CLParsingException(String message) {
            super(message);
        }

        public CLParsingException() {
        }
    }
    /**
     * Help option "-help", "-h", "-?"
     */
    public static final OptionDescr HELP = new OptionDescr("help", new String[]{"h", "?"}, "Basic options", null);
    /**
     * Help option for more verbose help "-help-verbose", "-hv"
     */
    public static final OptionDescr HELP_VERBOSE = new OptionDescr("help-verbose", new String[]{"hv"}, null, null);
    /**
     * JCov property file location "-propfile"
     */
    public static final OptionDescr PROPERTYFILE = new OptionDescr("propfile", "", OptionDescr.VAL_SINGLE, (String) null, (String) null);
    /**
     * Printing all environment and sources for each option value "-print-env",
     * "-env"
     */
    public static final OptionDescr PRINT_ENV = new OptionDescr("print-env", new String[]{"env"}, (String) null, (String) null);
    /**
     * JCov plugin directory location "-plugindir"
     */
    public static final OptionDescr PLUGINDIR = new OptionDescr("plugindir", "", OptionDescr.VAL_SINGLE, "Directory to read plugins (SPIs)", "plugins");
    /**
     * Redirecting logging to a file
     */
    public static final OptionDescr LOGFILE = new OptionDescr("log.file", "", OptionDescr.VAL_SINGLE, "Set file for output logging");
    /**
     * Setting verbosity
     */
    public static final OptionDescr LOGLEVEL = new OptionDescr("log.level", new String[]{"log"}, "", OptionDescr.VAL_SINGLE, "Set level of logging", "");
    static final List<OptionDescr> SHARED_OPTIONS = Arrays.asList(new OptionDescr[]{HELP, HELP_VERBOSE, PRINT_ENV, PROPERTYFILE, PLUGINDIR, LOGFILE, LOGLEVEL});
}
