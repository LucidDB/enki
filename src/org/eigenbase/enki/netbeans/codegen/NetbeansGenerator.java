/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007-2007 The Eigenbase Project
// Copyright (C) 2007-2007 Disruptive Tech
// Copyright (C) 2007-2007 LucidEra, Inc.
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
package org.eigenbase.enki.netbeans.codegen;

import java.util.*;

import org.eigenbase.enki.codegen.*;

/**
 * NetbeansGenerator generates JMI interfaces for a metamodel.  It is 
 * functionally equivalent to this sequence of Netbeans MDRANT tasks:
 * <pre>
 *   &lt;mdr&gt;
 *     &lt;instantiate name="ExtentName"/&gt;
 *     &lt;readXMI file="Model.xmi" extent="ExtentName"/&gt;
 *     &lt;mapJava dir="OutputDir" extent="ExtentName"/&gt;
 *   &lt;/mdr&gt;
 * </pre>  
 * 
 * Values for the extent name, model file and output directory are configured
 * via the base class methods {@link #setExtentName(String)}, 
 * {@link #setXmiFile(java.io.File)}, and 
 * {@link #setOutputDirectory(java.io.File)}.
 * 
 * @author Stephan Zuercher
 */
public class NetbeansGenerator
    extends MdrGenerator
{
    /**
     * The name of the generator option for including specific packages.
     */
    public static final String INCLUDE_PACKAGE_OPTION = "include";

    /**
     * Included package list.
     */
    private List<String> includedPackageList;
    
    public NetbeansGenerator()
    {
        super();
    }

    /**
     * Accepts the {@link #INCLUDE_PACKAGE_OPTION} option and ignores all others.
     * 
     * @param options map of option name to option value
     */
    @Override
    public void setOptions(Map<String, String> options)
    {
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
    }

    public static void main(String[] args)
    {
        NetbeansGenerator generator = new NetbeansGenerator();
        
        generator.doMain(args, true);
    }
}

// End NetbeansGenerator.java
