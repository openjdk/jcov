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

/**
 * This class offers a formal way of describing an option, e.g., whether it has
 * a value, whether it may be specified more than once, set of possible values
 * (if any), it's default value if any, it's usage information.
 *
 * @author Konstantin Bobrovsky
 */
public class OptionDescr {

    public final static int VAL_NONE = 0;
    public final static int VAL_SINGLE = 1;
    public final static int VAL_MULTI = 2;
    /**
     * <p> Option with VAL_ALL kind will take all values after it. </p> <p> E.g.
     * "-val_all_option val1 -val2 val3 -val4 -val5" will take all values
     * "val1", "-val2", "val3", "-val4", "-val5". </p>
     */
    public final static int VAL_ALL = 3;
    public final static String[][] ON_OFF = new String[][]{
        {"on", "on"},
        {"off", "off"}};
    public final static String ON = "on";
    public final static String OFF = "off";
    /**
     * option name
     */
    public String name;
    /**
     * option title (should more descriptive than the name)
     */
    public String title;
    /**
     * whether the option has a value
     */
    public boolean hasValue = false;
    /**
     * set of pairs : &lt;possible value&gt;, &lt;its description&gt;
     */
    public String[][] allowedValues;
    /**
     * whether the option may be specified more than once
     */
    public boolean isMultiple = false;
    /**
     * usage information
     */
    public String usage;
    /**
     * misc flags
     */
    public int flags;
    /**
     * default value
     */
    public String defVal;
    /**
     * aliases
     */
    public String[] aliases;
    public ServiceProvider registeringSPI;
    public int val_kind;

    /**
     * Default constructor
     */
    public OptionDescr() {
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - no value<br> - no possible
     * values<br> - no default value<br>
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String title, String usage) {
        this(name, title, VAL_NONE, null, usage, null);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - no value<br> - no possible
     * values<br> - default value<br>
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String title, String usage, String defValue) {
        this(name, title, VAL_NONE, null, usage, defValue);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - no value<br> - no possible
     * values<br> - no default value<br>
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String[] aliases, String title, String usage) {
        this(name, aliases, title, VAL_NONE, null, usage, null);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - can be specified only once
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name,
            String title,
            String[][] allowed_values,
            String usage,
            String def_val) {
        this(name, title, VAL_SINGLE, allowed_values, usage, def_val);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - have an alias - can be
     * specified only once
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name,
            String[] aliases,
            String title,
            String[][] allowed_values,
            String usage,
            String def_val) {
        this(name, aliases, title, VAL_SINGLE, allowed_values, usage, def_val);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - possible value is arbitrary<br>
     * - no default value
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String title, int val_kind, String usage) {
        this(name, title, val_kind, null, usage, null);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - possible value is arbitrary<br>
     * - no default value
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String title, int val_kind, String usage, String defValue) {
        this(name, title, val_kind, null, usage, defValue);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - aliases are defined - possible
     * value is arbitrary<br> - no default value
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String[] aliases, String title, int val_kind, String usage) {
        this(name, aliases, title, val_kind, null, usage, null);
    }

    /**
     * Creates new OptionDescr. Defaults :<br> - aliases are defined - possible
     * value is arbitrary<br> - no default value
     *
     * @see #OptionDescr(String, String, int, String[][], String, String)
     */
    public OptionDescr(String name, String[] aliases, String title, int val_kind, String usage, String defValue) {
        this(name, aliases, title, val_kind, null, usage, defValue);
    }

    /**
     * Creates new OptionDescr
     *
     * @param name option name
     * @param title option title (should more descriptive than the name)
     * @param val_kind one of<br>      <pre>
     *   <code>VAL_NONE</code> no value <code>VAL_SINGLE</code> has a value and
     * may be specified only once <code>VAL_MULTI</code> has a value and may be
     * specified multiple times
     * </pre>
     * @param allowed_values set of pairs : &lt;possible value&gt;, &lt;its
     * description&gt;
     * @param usage usage information
     * @param def_val default value
     */
    public OptionDescr(String name,
            String title,
            int val_kind,
            String[][] allowed_values,
            String usage,
            String def_val) {
        this(name, null, title, val_kind, allowed_values, usage, def_val);
    }

    /**
     * Creates new OptionDescr
     *
     * @param name option name
     * @param aliases option aliases
     * @param title option title (should be more descriptive than the name)
     * @param val_kind one of<br>      <pre>
     *   <code>VAL_NONE</code> no value <code>VAL_SINGLE</code> has a value and
     * may be specified only once <code>VAL_MULTI</code> has a value and may be
     * specified multiple times
     * </pre>
     * @param allowed_values set of pairs : &lt;possible value&gt;, &lt;its
     * description&gt;
     * @param usage usage information
     * @param def_val default value
     */
    public OptionDescr(String name,
            String[] aliases,
            String title,
            int val_kind,
            String[][] allowed_values,
            String usage,
            String def_val) {
        this.name = name;
        this.aliases = aliases;
        this.title = title;
        this.usage = usage;
        this.allowedValues = allowed_values;
        this.defVal = def_val;
        this.val_kind = val_kind;
        if (val_kind != VAL_NONE) {
            hasValue = true;
        }
        if (val_kind == VAL_MULTI || val_kind == VAL_ALL) {
            isMultiple = true;
        }
    }

    /**
     * Creates new OptionDescr, filling all fields from &lt;src&gt;
     *
     * @param src OptionDescr instance to copy fields from
     */
    public OptionDescr(OptionDescr src) {
        name = src.name;
        title = src.title;
        hasValue = src.hasValue;
        allowedValues = src.allowedValues;
        isMultiple = src.isMultiple;
        usage = src.usage;
        flags = src.flags;
        defVal = src.defVal;
    }

    /**
     * @return true if &lt;this&gt; equals o, false otherwise
     */
    public boolean equals(Object o) {
        if (!(o instanceof OptionDescr)) {
            return false;
        }
        OptionDescr od = (OptionDescr) o;
        return name.equals(od.name) && (title == null || od.title == null || title.equals(od.title));
    }

    public int hashCode() {
        int titleHash = title == null ? 0 : title.hashCode();
        return super.hashCode() + titleHash;
    }

    public boolean isName(String name) {
        if (name.equalsIgnoreCase(this.name)) {
            return true;
        }
        if (aliases == null) {
            return false;
        }
        for (int i = 0; i < aliases.length; i++) {
            if (name.equalsIgnoreCase(aliases[i])) {
                return true;
            }
        }
        return false;
    }
    /**
     * This method returns standard representation of usage. Syntax is: <prefix>
     * name (alias, alias, ...)<delimeter>'possible values', default is
     * (default). description.
     *
     */
    private final static String TAB = "    ";
    private final static String NL = "\n";

    public String getUsage(String prefix, String delimeter, String startTab,
            boolean verbose) {
        String usageString = "";
        if (title != null && title != "") {
            usageString = startTab + title + NL;
        }
        usageString += startTab + TAB + prefix + name;
        if (aliases != null && aliases.length > 0) {
            usageString += "(";
            for (int i = 0; i < aliases.length - 1; i++) {
                usageString += aliases[i] + ", ";
            }
            usageString += aliases[aliases.length - 1];
            usageString += ")";
        }
        if (this.hasValue) {
            usageString += delimeter;
            if (allowedValues != null && allowedValues.length > 0) {
                usageString += "[";
                for (int i = 0; i < allowedValues.length - 1; i++) {
                    usageString += allowedValues[i][0] + "|";
                }
                usageString += allowedValues[allowedValues.length - 1][0];
                usageString += "] (By default is: " + defVal + ")";
                if (verbose) {
                    for (int i = 0; i < allowedValues.length; i++) {
                        usageString += NL + startTab + TAB + TAB + allowedValues[i][0] + " : " + allowedValues[i][1];
                    }
                }
            } else {
                usageString += "'string value'";
                if (this.defVal != null) {
                    usageString += " (By default is: " + defVal + ")";
                }
                if (this.isMultiple) {
                    usageString += " (Option could be specified several times.)";
                }
            }
        }
        if (verbose) {
            if (usage != null && !"".equals(usage)) {
                usageString += NL + startTab + TAB + usage.replace(NL, NL + startTab + TAB);
            }
            if (registeringSPI != null) {
                usageString += NL + startTab + TAB + TAB + "registered by " + registeringSPI.getClass();
            }
            usageString += NL;
        }

        return usageString;
    }

    /**
     * Check whether value is allowed for this option
     *
     * @param value value to check
     * @return true if any value allowed (allowedValues is set to null) or if
     * value is in allowedValues; false if value is not in allowedValues
     */
    public boolean isAllowedValue(String value) {
        if (allowedValues == null) {
            return true;
        }

        for (String[] s : allowedValues) {
            if (s[0].equals(value)) {
                return true;
            }
        }
        return false;
    }

    public ServiceProvider getRegisteringSPI() {
        return registeringSPI;
    }

    public void setRegisteringSPI(ServiceProvider spi) {
        this.registeringSPI = spi;
    }
}