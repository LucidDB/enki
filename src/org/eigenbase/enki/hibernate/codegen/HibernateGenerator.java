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
package org.eigenbase.enki.hibernate.codegen;

import java.util.*;

import org.eigenbase.enki.codegen.*;

/**
 * HibernateGenerator is the main entry point for Hibernate code generation.  
 * It loads an XMI representation of a UML model via Netbeans MDR and produces:
 * <ol>
 * <li>Interfaces for the UML model.</li>
 * <li>Implementations of the interfaces that represent concrete classes.</li>
 * <li>
 *     A Hibernate mapping file to provide storage for those implementations.
 * </li>
 * </ol>
 * 
 * <p>Supported options:
 * <table border="1">
 * <tr>
 *   <th align="left">Name</th>
 *   <th align="left">Description</th>
 * </tr>
 * <tr>
 *   <td align="left">{@value #TABLE_PREFIX_OPTION}</td>
 *   <td align="left">
 *     A prefix to be used for all metamodel-specific tables.  Use is optional,
 *     but highly recommended.  Without this option only a single metamodel
 *     can be stored in a particular database schema.
 *   </td>
 * </tr>
 * <tr>
 *   <td align="left">{@value #DEFAULT_STRING_LENGTH_OPTION}</td>
 *   <td align="left">
 *     Controls the default length of string columns.  If unspecified, 
 *     the default is defined in 
 *     {@link CodeGenUtils#DEFAULT_STRING_LENGTH}. Optional.
 *   </td>
 * </tr>
 * <tr>
 *   <td align="left">{@value #IDENTIFIER_LIMIT_OPTION}</td>
 *   <td align="left">
 *     Controls the length of generated identifiers such as attribute and
 *     reference data members and their corresponding accessor methods.  This
 *     may be necessary for metamodels containing long object names; for
 *     example, MySQL column names can be 64 characters long at most.  Setting
 *     this limit too high can lead to errors during Hibernate mapping; setting
 *     this limit too low can lead to uniqueness clashes in generated
 *     identifiers (simple truncation is used, without mangling).
 *     Consequently, setting this limit may require experimentation for a given
 *     metamodel and DBMS combination (and requires upgrade consideration as
 *     well when metamodels change).  If unspecified, the default is
 *     unlimited.  Optional.
 *   </td>
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
public class HibernateGenerator extends MdrGenerator
{
    /** The name of the generator option for setting the table prefix. */
    public static final String TABLE_PREFIX_OPTION = "tablePrefix";
    
    /** 
     * The name of the generator option for setting the default string 
     * length. 
     */
    public static final String DEFAULT_STRING_LENGTH_OPTION = 
        "defaultStringLength";
    
    /** 
     * The name of the generator option for setting the identifier
     * limit. 
     */
    public static final String IDENTIFIER_LIMIT_OPTION = 
        "identifierLimit";
    
    /**
     * The name of the generator option for including specific packages.
     */
    public static final String INCLUDE_PACKAGE_OPTION = "include";

    /** The name of the plug-in mode option. */
    public static final String PLUGIN_OPTION = "plugin";
    
    /** Prefix for all table names in this metamodel. */
    private String tablePrefix;
    
    /** 
     * Default string length. A value of -1 indicates that no value has
     * been set.
     */
    private int defaultStringLength = -1;

    /**
     * Identifier limit.  A value of -1 indicates that no limit
     * has been set.
     */
    private int identifierLimit = -1;
    
    /**
     * Included package list.
     */
    private List<String> includedPackageList;
    
    /** Plug-in flag. */
    private boolean pluginMode;
    
    public HibernateGenerator()
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
        tablePrefix = options.get(TABLE_PREFIX_OPTION);
        
        if (tablePrefix == null || tablePrefix.length() == 0) {
            System.err.println(
                "WARNING: use of tablePrefix option is highly recommended");
            tablePrefix = null;
        }
        
        String lengthValue = options.get(DEFAULT_STRING_LENGTH_OPTION);
        if (lengthValue != null) {
            defaultStringLength = Integer.parseInt(lengthValue);
        }
        
        lengthValue = options.get(IDENTIFIER_LIMIT_OPTION);
        if (lengthValue != null) {
            identifierLimit = Integer.parseInt(lengthValue);
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
        
        String pluginModeValue = options.get(PLUGIN_OPTION);
        if (pluginModeValue != null) {
            pluginMode = Boolean.parseBoolean(pluginModeValue);
        }
    }
    
    /**
     * Configures a {@link JmiTemplateHandler} for UML interfaces, a
     * {@link HibernateJavaHandler} for Hibernate entities and a 
     * {@link HibernateMappingHandler} to generate a Hibernate mapping file.
     */
    @Override
    protected void configureHandlers()
    {
        JmiTemplateHandler jmiTemplateHandler = new JmiTemplateHandler();
        jmiTemplateHandler.setIncludes(includedPackageList);
        addHandler(jmiTemplateHandler);
        
        HibernateJavaHandler javaHandler = new HibernateJavaHandler();
        javaHandler.setIncludes(includedPackageList);
        javaHandler.setPluginMode(pluginMode);
        javaHandler.setTablePrefix(tablePrefix);
        addHandler(javaHandler);
        
        HibernateMappingHandler mappingHandler = new HibernateMappingHandler();
        mappingHandler.setExtentName(getExtentName());
        mappingHandler.setTablePrefix(tablePrefix);
        mappingHandler.setIncludes(includedPackageList);
        mappingHandler.setPluginMode(pluginMode);
        if (defaultStringLength != -1) {
            mappingHandler.setDefaultStringLength(defaultStringLength);
        }
        addHandler(mappingHandler);
        
        HibernateMofInitHandler metamodelInitHandler = 
            new HibernateMofInitHandler(mappingHandler);
        metamodelInitHandler.setIncludes(includedPackageList);
        metamodelInitHandler.setPluginMode(pluginMode);
        addHandler(metamodelInitHandler);
    }
    
    @Override
    public String transformIdentifier(String identifier)
    {
        // TODO jvs 30-Jun-2008:  something smarter than
        // just truncation
        
        identifier = super.transformIdentifier(identifier);
        if (identifierLimit != -1) {
            if (identifier.length() > identifierLimit) {
                identifier = identifier.substring(0, identifierLimit);
            }
        }
        return identifier;
    }

    // REVIEW jvs 30-Jun-2008:  without Any what?
    /**
     * Provides an entry point for testing without Any.
     * 
     * @param args
     */
    public static void main(String[] args)
    {
        HibernateGenerator generator = new HibernateGenerator();
        
        generator.doMain(args, true);
    }
}
