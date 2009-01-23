/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009-2009 The Eigenbase Project
// Copyright (C) 2009-2009 Disruptive Tech
// Copyright (C) 2009-2009 LucidEra, Inc.
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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

/**
 * AssociationEndBase provides implementations for operations for Enki's 
 * internal {@link AssociationEnd} implementation.  
 * 
 * @author Stephan Zuercher
 */
public abstract class AssociationEndBase extends ModelElementBase
{
    protected AssociationEndBase(RefClass refClass)
    {
        super(refClass);
    }
    
    protected AssociationEndBase()
    {
        super();
    }
    
    protected AssociationEndBase(MetamodelInitializer initializer)
    {
        super(initializer);
    }
    
    protected AssociationEnd otherEnd()
    {
        AssociationEnd thisEnd = (AssociationEnd)this;
        
        Namespace container = thisEnd.getContainer();
        for(Object o: container.getContents()) {
            if (o instanceof AssociationEnd && o != thisEnd) {
                return (AssociationEnd)o;
            }
        }
        
        // All associations have two ends, so this shouldn't be reachable
        throw new InternalJmiError("Association must have an 'other end'");
    }
}

// End AssociationEndBase.java
