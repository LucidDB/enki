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

import java.io.*;

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
    public HibernateGenerator()
    {
        super();
    }
    
    @Override
    protected void configureHandlers()
    {
        JmiTemplateHandler jmiTemplateHandler = new JmiTemplateHandler();
        addHandler(jmiTemplateHandler);
        
        HibernateJavaHandler javaHandler = new HibernateJavaHandler();
        addHandler(javaHandler);
        
        String prefix = xmiFile.getName();
        int dot = prefix.indexOf('.');
        if (dot > 0) {
            prefix = prefix.substring(0, dot);
        }
        HibernateMappingHandler mappingHandler = new HibernateMappingHandler();
        mappingHandler.setMappingFilePrefix(prefix);
        addHandler(mappingHandler);
    }

    public static void main(String[] args)
    {
        try {
            String xmiFileName = args[0];
            String outputDirName = args[1];

            HibernateGenerator g = new HibernateGenerator();
            g.setXmiFile(new File(xmiFileName));
            g.setOutputDirectory(new File(outputDirName));
            g.setUseGenerics(true);
            g.execute();
        }
        catch(Exception e) {
            System.err.println(
                "Usage: java " + HibernateGenerator.class.toString() + 
                " <xmi-file> <out-dir>");
            System.err.println();
            e.printStackTrace();
        }
    }
}
