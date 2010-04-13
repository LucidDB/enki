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
 * ModelElementBase provides implementations for operations for Enki's internal
 * {@link ModelElement} implementation.  
 * 
 * @author Stephan Zuercher
 */
public abstract class ModelElementBase extends RefObjectBase
{
    public static final HashSet<String> ALL_DEP_KINDS = 
        new HashSet<String>(
            Arrays.asList(
                ModelElement.CONSTRAINTDEP,
                ModelElement.CONTAINERDEP,
                ModelElement.CONSTRAINEDELEMENTSDEP,
                ModelElement.SPECIALIZATIONDEP,
                ModelElement.IMPORTDEP,
                ModelElement.CONTENTSDEP,
                ModelElement.SIGNATUREDEP,
                ModelElement.TAGGEDELEMENTSDEP,
                ModelElement.TYPEDEFINITIONDEP,
                ModelElement.REFERENCEDENDSDEP));

    private List<String> qualifiedName;
    
    protected ModelElementBase(RefClass refClass)
    {
        super(refClass);
    }
    
    protected ModelElementBase()
    {
        super();
    }
    
    protected ModelElementBase(MetamodelInitializer initializer)
    {
        super(initializer);
    }

    // Implement ModelElement
    protected synchronized List<String> getQualifiedName()
    {
        if (qualifiedName == null) {
            ArrayList<String> name = new ArrayList<String>();
        
            ModelElement modelElement = (ModelElement)this;
            name.add(modelElement.getName());
        
            Namespace ns = modelElement.getContainer();
            while(ns != null) {
                name.add(ns.getName());
                ns = ns.getContainer();
            }
        
            Collections.reverse(name);
        
            qualifiedName = name;
        }

        return qualifiedName;
    }
    
    // Implement ModelElement
    protected Collection<ModelElement> findRequiredElements(
        Collection<String> kinds,
        boolean recursive)
    {
        if (kinds.contains(ModelElement.ALLDEP)) {
            kinds = ALL_DEP_KINDS;
        }
        
        if (recursive) {
            HashSet<ModelElement> seen = new HashSet<ModelElement>();
            seen.add((ModelElement)this);
            recursiveFindDeps(kinds, seen);
            return seen;
        } else {
            HashSet<ModelElement> deps = new HashSet<ModelElement>();
            for(String kind: kinds) {
                Collection<ModelElement> depsOfKind = findDepsOfKind(kind);
                deps.addAll(depsOfKind);
            }
            return deps;
        }
    }

    // Implement ModelElement
    protected boolean isRequiredBecause(ModelElement other, String[] reason)
    {
        // OUT_DIR param of multiplicity 1
        assert(reason != null && reason.length == 1);

        for(String kind: ALL_DEP_KINDS) {
            if (isDepOfKind(kind, other)) {
                reason[0] = kind;
                return true;
            }
        }
        
        if (!findRequiredElements(ALL_DEP_KINDS, true).isEmpty()) {
             reason[0] = ModelElement.INDIRECTDEP;
             return true;
        }
        
        reason[0] = "";
        return false;
    }

    // Implement ModelElement
    protected boolean isFrozen()
    {
        return true;
    }

    // Implement ModelElement
    protected boolean isVisible(ModelElement otherElement)
    {
        // JMI spec says this is reserved for future use.  Return true for 
        // now.
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private Collection<ModelElement> findDepsOfKind(String kind)
    {
        HashSet<ModelElement> result = new HashSet<ModelElement>();

        if (kind.equals(ModelElement.CONSTRAINTDEP)) {
            result.addAll(((ModelElement)this).getConstraints());
        } else if (kind.equals(ModelElement.CONTAINERDEP)) {
            result.add(((ModelElement)this).getContainer());
        } else if (kind.equals(ModelElement.CONSTRAINEDELEMENTSDEP) && 
                   (this instanceof Constraint))
        {
            result.addAll(((Constraint)this).getConstrainedElements());
        } else if (kind.equals(ModelElement.SPECIALIZATIONDEP) && 
                   (this instanceof GeneralizableElement))
        {
            result.addAll(((GeneralizableElement)this).getSupertypes());
        } else if (kind.equals(ModelElement.IMPORTDEP) && 
                   (this instanceof Import))
        {
            result.add(((Import)this).getImportedNamespace());
        } else if (kind.equals(ModelElement.CONTENTSDEP) && 
                   (this instanceof Namespace))
        {
            result.addAll(((Namespace)this).getContents());
        } else if (kind.equals(ModelElement.SIGNATUREDEP) &&
                   (this instanceof Operation))
        {
            result.addAll(((Operation) this).getExceptions());
        } else if (kind.equals(ModelElement.TAGGEDELEMENTSDEP) && 
                   (this instanceof Tag))
        {
            result.addAll(((Tag)this).getElements());
        } else if (kind.equals(ModelElement.TYPEDEFINITIONDEP) &&
                   (this instanceof TypedElement))
        {
            result.add(((TypedElement)this).getType());
        } else if (kind.equals(ModelElement.REFERENCEDENDSDEP) &&
                   (this instanceof Reference))
        {
            result.add(((Reference)this).getReferencedEnd());
            result.add(((Reference)this).getExposedEnd());
        }

        result.remove(null);
        
        return result;
    }
    
    private boolean isDepOfKind(String kind, ModelElement other)
    {
        return findDepsOfKind(kind).contains(other);
    }
    
    private void recursiveFindDeps(
        Collection<String> kinds, Set<ModelElement> seen)
    {
        HashSet<ModelElement> seen2 = new HashSet<ModelElement>();
        
        for(String kind: kinds) {
            Collection<ModelElement> depsOfKind = findDepsOfKind(kind);
            seen2.addAll(depsOfKind);
        }
        
        // MOF spec has an "if seen = seen2".  Instead, just add all of seen2
        // into seen and recurse if there are new elements.
        for(Iterator<ModelElement> iter = seen2.iterator(); iter.hasNext(); ) {
            ModelElement elem = iter.next();
            
            if (!seen.add(elem)) {
                // Seen already contained elem.
                iter.remove();
            }
        }
        
        if (!seen2.isEmpty()) {
            for(ModelElement elem: seen2) {
                ((ModelElementBase)elem).recursiveFindDeps(kinds, seen);
            }
        }
    }
}

// End ModelElementBase.java
