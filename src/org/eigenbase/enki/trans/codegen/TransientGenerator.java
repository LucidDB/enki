/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 2.1 of the License, or (at
// your option) any later version.
// 
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
*/
package org.eigenbase.enki.trans.codegen;

import java.util.*;

import org.eigenbase.enki.codegen.*;
import org.eigenbase.enki.hibernate.codegen.*;

/**
 * TransientGenerator is the main entry point for generating code to work
 * with the Enki/Transient repository implementation.
 * 
 * <p>Supported options:
 * <table border="1">
 * <tr>
 *   <th align="left">Name</th>
 *   <th align="left">Description</th>
 * </tr>
 * <tr>
 *   <td align="left">{@value #INCLUDE_PACKAGE_OPTION}</td>
 *   <td align="left">
 *     List of packages (by fully-qualified Java name) to include.  All
 *     other packages are excluded.  If unspecified, all packages are 
 *     included.  Optional.
 *   </td>
 * </tr>
 * <tr>
 *   <td align="left">{@value #PLUGIN_OPTION}</td>
 *   <td align="left">
 *     Boolean flag to control whether the generated code is a plugin 
 *     (requiring a base model generated separately).  Defaults to false.
 *     Optional.
 *   </td>
 * </tr>
 * </table>
 * 
 * @author Stephan Zuercher
 */
public class TransientGenerator
    extends MdrGenerator
{
    /** The name of the plug-in mode option. */
    public static final String PLUGIN_OPTION = "plugin";
    
    /**
     * Name of the generator option that controls including specific packages.
     */
    public static final String INCLUDE_PACKAGE_OPTION = "include";

    /** Plug-in flag. */
    private boolean pluginMode;
    
    /**
     * Included package list.
     */
    private List<String> includedPackageList;
    
    public TransientGenerator()
    {
        super();
    }

    /**
     * Accepts the options described in {@link HibernateGenerator} and ignores 
     * all others.
     * 
     * @param options map of option name to option value
     */
    @Override
    public void setOptions(Map<String, String> options)
    {
        String pluginModeValue = options.get(PLUGIN_OPTION);
        if (pluginModeValue != null) {
            pluginMode = Boolean.parseBoolean(pluginModeValue);
        }
        
        String includedPackages = options.get(INCLUDE_PACKAGE_OPTION);
        if (includedPackages != null) {
            // Accept package name with slashes
            includedPackages = includedPackages.replace("/", ".");
            String[] names = includedPackages.split(",");
            includedPackageList = new ArrayList<String>();
            for(String name: names) {
                includedPackageList.add(name.trim());
            }
            includedPackageList = 
                Collections.unmodifiableList(includedPackageList);
        }
    }
    
    @Override
    protected void configureHandlers()
    {
        JmiTemplateHandler jmiTemplateHandler = new JmiTemplateHandler();
        jmiTemplateHandler.setIncludes(includedPackageList);
        addHandler(jmiTemplateHandler);
        
        TransientHandler transientHandler = new TransientHandler();
        transientHandler.setExtentName(getExtentName());
        transientHandler.setPluginMode(pluginMode);
        transientHandler.setIncludes(includedPackageList);
        addHandler(transientHandler);
        
        // TODO: HibernateMofInitHandler is actually pretty generic. Factor
        // out the one Hibernate-ism (IMPL_SUFFIX from HibernateJavaHandler)
        // and move it elsewhere.
        MofInitHandler mofInitHandler = 
            new HibernateMofInitHandler(transientHandler);
        mofInitHandler.setPluginMode(pluginMode);
        mofInitHandler.setIncludes(includedPackageList);
        addHandler(mofInitHandler);
    }

    public static void main(String[] args)
    {
        TransientGenerator generator = new TransientGenerator();
        
        generator.doMain(args, true);
    }
}

// End TransientGenerator.java
