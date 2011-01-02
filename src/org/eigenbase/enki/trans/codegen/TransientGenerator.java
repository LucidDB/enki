/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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
 * <tr>
 *   <td align="left">{@value #JMI_INTERFACES_OPTION}</td>
 *   <td align="left">
 *     Boolean flag to control whether the generated code includes the JMI
 *     interfaces (requiring them to be generated separately).
 *     Optional, defaults to {@value #DEFAULT_JMI_INTERFACES_OPTION}.
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

    /**
     * Name of the generator option that controls whether JMI interfaces are
     * generated.  Defaults to {@link #DEFAULT_JMI_INTERFACES_OPTION}.
     */
    public static final String JMI_INTERFACES_OPTION = "jmiInterfaces";

    /**
     * Default value for {@link #JMI_INTERFACES_OPTION}.
     */
    public static final boolean DEFAULT_JMI_INTERFACES_OPTION = true;

    public static final String IMPL_SUFFIX = "$Trans";
    
    /** Plug-in flag. */
    private boolean pluginMode;

    /** JMI interfaces option. */
    private boolean jmiInterfaces;
    
    /**
     * Included package list.
     */
    private List<String> includedPackageList;
    
    public TransientGenerator()
    {
        super();
        
        jmiInterfaces = DEFAULT_JMI_INTERFACES_OPTION;
    }

    @Override
    public String getImplSuffix()
    {
        return IMPL_SUFFIX;
    }
    
    /**
     * Accepts the options described in {@link TransientGenerator} and ignores 
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

        String jmiInterfacesOption = options.get(JMI_INTERFACES_OPTION);
        if (jmiInterfacesOption != null) {
            jmiInterfaces = Boolean.parseBoolean(jmiInterfacesOption);
        } else {
            jmiInterfaces = DEFAULT_JMI_INTERFACES_OPTION;
        }
    }
    
    @Override
    protected void configureHandlers()
    {
        if (jmiInterfaces) {
            JmiTemplateHandler jmiTemplateHandler = new JmiTemplateHandler();
            jmiTemplateHandler.setIncludes(includedPackageList);
            addHandler(jmiTemplateHandler);
        }
        
        TransientHandler transientHandler = new TransientHandler();
        transientHandler.setExtentName(getExtentName());
        transientHandler.setPluginMode(pluginMode);
        transientHandler.setIncludes(includedPackageList);
        addHandler(transientHandler);
        
        MetamodelInitHandler metamodelInitHandler =
            new MetamodelInitHandler(transientHandler);
        metamodelInitHandler.setPluginMode(pluginMode);
        metamodelInitHandler.setIncludes(includedPackageList);
        addHandler(metamodelInitHandler);
    }

    public static void main(String[] args)
    {
        TransientGenerator generator = new TransientGenerator();
        
        generator.doMain(args, true);
    }
}

// End TransientGenerator.java
