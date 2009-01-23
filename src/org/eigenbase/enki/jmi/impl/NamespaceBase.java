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

import java.util.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

/**
 * NamespaceBase provides implementations for operations for Enki's internal
 * {@link Namespace} implementation.  
 * 
 * @author Stephan Zuercher
 */
public abstract class NamespaceBase
    extends ModelElementBase
{
    protected NamespaceBase(RefClass refClass)
    {
        super(refClass);
    }
    
    protected NamespaceBase()
    {
        super();
    }
    
    protected NamespaceBase(MetamodelInitializer initializer)
    {
        super(initializer);
    }

    // Implement Namespace
    protected ModelElement lookupElement(String name)
    throws NameNotFoundException
    {
        for(Object o: ((Namespace)this).getContents()) {
            ModelElement modelElement = (ModelElement)o;
            
            if (modelElement.getName().equals(name)) {
                return modelElement;
            }
        }
        
        throw new NameNotFoundException(name);
    }

    // Implement Namespace
    protected ModelElement resolveQualifiedName(List<?> qualifiedName)
    throws NameNotResolvedException
    {
        if (qualifiedName == null || qualifiedName.isEmpty()) {
            throw new NameNotResolvedException(
                "no qualified name", qualifiedName);
        }

        Namespace ns = (Namespace)this;
        
        NAME_LOOP:
        for(int i = 0; i < qualifiedName.size(); i++) {
            String name = qualifiedName.get(i).toString();
            
            for(Object o: ns.getContents()) {
                if (o instanceof ModelElement) {
                    ModelElement modelElement = (ModelElement)o;
                    if (name.equals(modelElement.getName())) {
                        if (i + 1 >= qualifiedName.size()) {
                            // Found a matching model element and there's no
                            // more work to do.
                            return modelElement;
                        } else if (modelElement instanceof Namespace) {
                            // Found a matching Namespace, continue to the
                            // next qualified name element.
                            ns = (Namespace)modelElement;
                            continue NAME_LOOP;
                        }
                        
                        throw new NameNotResolvedException(
                            modelElement.getName() + " is not a Namespace", 
                            qualifiedName.subList(i, qualifiedName.size()));
                    }
                }
            }
            
            throw new NameNotResolvedException(
                name + " not found",
                qualifiedName.subList(i, qualifiedName.size()));
        }

        // Should be possible to get here.
        throw new NameNotResolvedException("internal error", qualifiedName);
    }

    // Implement Namespace
    protected List<ModelElement> findElementsByType(
        MofClass ofType,
        boolean includeSubtypes)
    {
        ArrayList<ModelElement> result = new ArrayList<ModelElement>();
        
        for(Object o: ((Namespace)this).getContents()) {
            ModelElement modelElem = (ModelElement)o;
            
            if (modelElem.refIsInstanceOf(ofType, includeSubtypes)) {
                result.add(modelElem);
            }
        }

        return result;
    }

    // Implement Namespace
    protected boolean nameIsValid(String proposedName)
    {
        Namespace ns = (Namespace)this;
        
        Collection<ModelElement> extendedNamespace = extendedNamespace(ns);

        for(ModelElement modelElement: extendedNamespace) {
            if (modelElement.getName().equals(proposedName)) {
                return false;
            }
        }
        
        return true;
    }
    
    protected Collection<ModelElement> extendedNamespace(Namespace ns)
    {
        LinkedHashSet<ModelElement> extendedNamespace = 
            new LinkedHashSet<ModelElement>();
        
        MofPackage mofPkg = null;
        GeneralizableElement genElem = null;
        if (this instanceof GeneralizableElement) {
            genElem = (GeneralizableElement)this;

            if (this instanceof MofPackage) {
                mofPkg = (MofPackage)this;
            }
        }
        
        for(Object containedElemObj: ns.getContents()) {
            ModelElement containedElem = (ModelElement)containedElemObj;
        
            extendedNamespace.add(containedElem);

            if (mofPkg != null && containedElem instanceof Import) {
                Import imp = (Import)containedElem;
                
                extendedNamespace.add(imp.getImportedNamespace());
            }
        }
        
        if (genElem != null) {
            for(Object supertypeObj: genElem.allSupertypes()) {
                Namespace supertype = (Namespace)supertypeObj;
                
                for(Object containedElemObj: supertype.getContents()) {
                    ModelElement containedElem = 
                        (ModelElement)containedElemObj;
                    
                    extendedNamespace.add(containedElem);
                    
                    if (mofPkg != null && containedElem instanceof Import) {
                        Import imp = (Import)containedElem;
                        
                        extendedNamespace.add(imp);
                    }
                }
            }
        }

        return extendedNamespace;
    }
}

// End NamespaceBase.java
