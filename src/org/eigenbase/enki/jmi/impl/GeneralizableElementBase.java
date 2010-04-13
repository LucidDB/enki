/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2009 The Eigenbase Project
// Copyright (C) 2009 SQLstream, Inc.
// Copyright (C) 2009 Dynamo BI Corporation
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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

/**
 * GeneralizableElementBase provides implementations for operations for Enki's
 *  internal {@link GeneralizableElement} implementation.  
 * 
 * @author Stephan Zuercher
 */
public abstract class GeneralizableElementBase
    extends NamespaceBase
{
    private List<GeneralizableElement> allSupertypes;
    
    protected GeneralizableElementBase(RefClass refClass)
    {
        super(refClass);
    }
    
    protected GeneralizableElementBase()
    {
        super();
    }
    
    protected GeneralizableElementBase(MetamodelInitializer initializer)
    {
        super(initializer);
    }
    
    // Implement GeneralizableElement
    protected synchronized List<GeneralizableElement> allSupertypes()
    {
        if (allSupertypes == null) {
            LinkedHashSet<GeneralizableElement> supertypes = 
                new LinkedHashSet<GeneralizableElement>();
    
            addAllSupertypes((GeneralizableElement)this, supertypes);
            
            // Convert to List
            allSupertypes = new ArrayList<GeneralizableElement>(supertypes);
        }
        
        return allSupertypes;
    }
    
    // Implement GeneralizableElement
    protected ModelElement lookupElementExtended(String name) 
    throws NameNotFoundException
    {
        for(ModelElement modelElement: extendedNamespace((Namespace)this)) {
            if (modelElement.getName().equals(name)) {
                return modelElement;
            }
        }
        
        throw new NameNotFoundException(name);
    }
    
    // Implement GeneralizableElement
    protected java.util.List<ModelElement> findElementsByTypeExtended(
        MofClass ofType, boolean includeSubtypes)
    {
        ArrayList<ModelElement> result = new ArrayList<ModelElement>();
        
        for(ModelElement modelElem : extendedNamespace((Namespace)this)) {
            if (modelElem.refIsInstanceOf(ofType, includeSubtypes)) {
                result.add(modelElem);
            }
        }

        return result;
    }

    private static void addAllSupertypes(
        GeneralizableElement type, 
        LinkedHashSet<GeneralizableElement> supertypes)
    {
        for(Object o: type.getSupertypes()) {
            GeneralizableElement genElem = (GeneralizableElement)o;
            
            addAllSupertypes(genElem, supertypes);
            supertypes.add(genElem);
        }
    }
}

// End GeneralizableElementBase.java
