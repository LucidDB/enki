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
 * Generator is the main entry point for Hibernate code generation.  It
 * loads an XMI representation of a UML model via Netbeans MDR and produces:
 * <ol>
 * <li>Interfaces for the UML model.</li>
 * <li>Implementations of the interfaces that represent concrete classes.</li>
 * <li>
 *     A Hibernate mapping file to provide storage for those implementations.
 * </li>
 * </ol>
 */
public class HibernateGenerator extends MdrGenerator
{
    /** The name of the generator option for setting the table prefix. */
    public static final String TABLE_PREFIX_OPTION = "tablePrefix";
    
    /** Prefix for all table names in this metamodel. */
    private String tablePrefix;
    
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
