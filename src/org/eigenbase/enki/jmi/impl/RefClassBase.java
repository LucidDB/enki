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
package org.eigenbase.enki.jmi.impl;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;

/**
 * RefClassBase is a base class for {@link RefClass} implementations.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefClassBase extends RefFeaturedBase implements RefClass
{
    private final RefPackage container;
    private final EnkiMDRepository repos;
    
    protected RefClassBase(RefPackage container)
    {
        super();
        
        this.container = container;
        this.repos = getCurrentInitializer().getRepository();
    }
    
    // Implement RefBaseObject
    public RefPackage refImmediatePackage()
    {
        return container;
    }
    
    @SuppressWarnings("unchecked")
    public Collection refAllOfClass()
    {
        logJmi("refAllOfClass");
        
        return getInitializer().getAllInstancesOf(this, false);
    }

    @SuppressWarnings("unchecked")
    public Collection refAllOfType()
    {
        logJmi("refAllOfClass");
        
        return getInitializer().getAllInstancesOf(this, true);
    }

    @SuppressWarnings("unchecked")
    public RefObject refCreateInstance(List params)
    {
        logJmi("refCreateInstance");
        
        return createInstance(refMetaObject(), null, params, RefObject.class);
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(RefObject struct, List params)
    {
        logJmi("refCreateStruct(ByRefObject)");
        
        return createInstance(struct, null, params, RefStruct.class);
    }

    @SuppressWarnings("unchecked")
    public RefStruct refCreateStruct(String struct, List params)
    {
        logJmi("refCreateStruct(ByName)");
        
        return createInstance(null, struct, params, RefStruct.class);
    }

    public RefEnum refGetEnum(RefObject enumType, String literalName)
    {
        return getEnum(enumType, null, literalName);
    }

    public RefEnum refGetEnum(String enumTypeName, String literalName)
    {
        return getEnum(null, enumTypeName, literalName);
    }
    
    @Override
    public EnkiMDRepository getRepository()
    {
        return repos;
    }
}

// End RefClassBase.java
