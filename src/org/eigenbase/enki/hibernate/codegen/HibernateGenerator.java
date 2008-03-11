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
 *   <td align="left">tablePrefix</td>
 *   <td align="left">
 *     A prefix to be used for all metamodel-specific tables.  Use is optional,
 *     but highly recommended.  Without this option only a single metamodel
 *     can be stored in a particular database schema.
 *   </td>
 * </tr>
 * <tr>
 *   <td align="left">defaultStringLength</td>
 *   <td align="left">
 *     Controls the default length of string columns.  The default is 
 *     defined in {@link HibernateMappingHandler#DEFAULT_STRING_LENGTH}.
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

    /** Prefix for all table names in this metamodel. */
    private String tablePrefix;
    
    /** 
     * Default string length. A value of -1 indicates that no value has
     * been set.
     */
    private int defaultStringLength = -1;
    
    public HibernateGenerator()
    {
        super();
    }
    
    /**
     * Accepts the {@link #TABLE_PREFIX_OPTION} option and ignores all others.
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
            try {
                defaultStringLength = Integer.parseInt(lengthValue);
            }
            catch(NumberFormatException e) {
                System.err.println(
                    "WARNING: Unable to parse default string length: " + 
                    lengthValue);
            }
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
        addHandler(jmiTemplateHandler);
        
        HibernateJavaHandler javaHandler = new HibernateJavaHandler();
        addHandler(javaHandler);
        
        HibernateMappingHandler mappingHandler = new HibernateMappingHandler();
        mappingHandler.setExtentName(getExtentName());
        mappingHandler.setTablePrefix(tablePrefix);
        if (defaultStringLength != -1) {
            mappingHandler.setDefaultStringLength(defaultStringLength);
        }
        addHandler(mappingHandler);
        
        MofInitHandler metamodelInitHandler = 
            new MofInitHandler(mappingHandler);
        addHandler(metamodelInitHandler);
    }

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
