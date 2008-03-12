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
package org.eigenbase.enki.codegen;

import java.util.*;

/**
 * MofImplementationHandler is used to generate classes in the
 * {@link org.eigenbase.enki.jmi.impl} package.  This generator is typically 
 * not used except during development.
 * 
 * @author Stephan Zuercher
 */
public class MofImplementationHandler
    extends TransientImplementationHandler
{
    public MofImplementationHandler()
    {
        super();
    }
    
    @Override
    public void setIncludes(List<String> includedPackages)
    {
        throw new UnsupportedOperationException(
            "MofImplementatinHandler does not support explicit package inclusion");
    }


    protected String convertToTypeName(String entityName) 
        throws GenerationException
    {
        if (!entityName.startsWith(JMI_PACKAGE_PREFIX)) {
            throw new GenerationException(
                "All types are expected to be within " + JMI_PACKAGE_PREFIX);
        }
        
        entityName = 
            JMI_PACKAGE_PREFIX_SUBST + 
            entityName.substring(JMI_PACKAGE_PREFIX.length());
        
        return entityName;
    }

    protected String computeSuffix(String baseSuffix)
    {
        return baseSuffix;
    }
}

// End MofImplementationHandler.java
