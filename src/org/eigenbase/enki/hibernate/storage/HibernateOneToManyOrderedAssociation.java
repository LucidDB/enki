/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
package org.eigenbase.enki.hibernate.storage;

import java.util.*;

/**
 * HibernateOneToManyOrderedAssociation extends 
 * HibernateOneToManyAssociationBase to provide a base class that stores 
 * one-to-many associations.  It is extended per-model to provide separate
 * storage for each model's associations. Specifically, this class handles 
 * 0..1 to 0..*, 1 to 0..*, 0..1 to 1..*, and 1 to 1..* associations where
 * the many end is ordered.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyOrderedAssociation
    extends HibernateOneToManyAssociationBase
    implements HibernateOrderedAssociation
{
    private List<HibernateAssociable> children;
    
    public HibernateOneToManyOrderedAssociation()
    {
        super();
        this.children = new ArrayList<HibernateAssociable>();
    }

    /*
     * N.B. Uniqueness is a MOF constraint and is NOT enforced by Netbeans
     * MDR.
     */
    public boolean getUnique()
    {
        return false;
    }
    
    public List<HibernateAssociable> getChildren()
    {
        return children;
    }
    
    public void setChildren(List<HibernateAssociable> children)
    {
        this.children = children;
    }

    @SuppressWarnings("unchecked")
    public <E> List<E> getChildren(Class<E> cls)
    {
        for(Object child: children) {
            cls.cast(child);
        }

        return (List<E>)children;
    }

    protected final Collection<HibernateAssociable> getCollection()
    {
        return getChildren();
    }
    
    public void add(
        int index, HibernateAssociable end1, HibernateAssociable end2)
    {
        final String type = getType();

        HibernateAssociable newParent;
        HibernateAssociable newChild;
        boolean parentIsFirstEnd;
        if (getReversed()) {
            newParent = end2;
            newChild = end1;
            parentIsFirstEnd = false;
        } else {
            newParent = end1;
            newChild = end2;
            parentIsFirstEnd = true;

        }
        boolean childIsFirstEnd = !parentIsFirstEnd;
        
        // This association must be related to one of the two objects.
        HibernateOneToManyOrderedAssociation parentAssoc = 
            (HibernateOneToManyOrderedAssociation)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyOrderedAssociation childAssoc = 
            (HibernateOneToManyOrderedAssociation)newChild.getAssociation(
                type, childIsFirstEnd);

        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            if (sameChild && getUnique()) {
                return;
            }

            if (childAssoc != null && !sameChild) {
                // Child associated with another parent.
                childAssoc.removeAll(newChild, false);
            }
            
            newChild.setAssociation(type, childIsFirstEnd, this);
            getChildren().add(index, newChild);
            return;
        }

        if (parentAssoc == null) {
            // Parent had no previous association.
            newParent.setAssociation(type, parentIsFirstEnd, this);
            setParent(newParent);
            return;
        }
        
        // Associating child with a new parent.  Invoke parent association's
        // add method.
        parentAssoc.add(index, end1, end2);
    }

    public boolean remove(
        int index, HibernateAssociable end1, HibernateAssociable end2)
    {
        HibernateAssociable parent;
        HibernateAssociable child;
        if (getReversed()) {
            parent = end2;
            child = end1;
        } else {
            parent = end1;
            child = end2;
        }

        return removeInternal(parent, child, index);
    }
    
    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        return getOrdered(item);
    }
    
    public List<HibernateAssociable> getOrdered(HibernateAssociable item)
    {
        if (!equals(item, getParent())) {
            return Collections.emptyList();
        }
        
        return getChildren();
    }
}

// End HibernateOneToManyOrderedAssociation.java
