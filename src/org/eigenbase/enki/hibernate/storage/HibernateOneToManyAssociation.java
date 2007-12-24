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
package org.eigenbase.enki.hibernate.storage;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;

/**
 * HibernateOneToManyAssociation extends HibernateAssociation and stores
 * one-to-many associations.  Specifically this class handles 0..1 to 0..*, 
 * 1 to 0..*, 0..1 to 1..*, and 1 to 1..* associations. 
 * 
 * @author Stephan Zuercher
 */
public class HibernateOneToManyAssociation
    extends HibernateAssociation
{
    private HibernateAssociable parent;
    private List<HibernateAssociable> children;
    
    public HibernateOneToManyAssociation()
    {
        this.children = new ArrayList<HibernateAssociable>();
    }
    
    public HibernateAssociable getParent()
    {
        return parent;
    }

    public void setParent(HibernateAssociable parent)
    {
        this.parent = parent;
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

    @Override
    public boolean add(
        HibernateAssociable newParent, HibernateAssociable newChild)
    {
        final String type = getType();

        // This association must be related to one of the two objects.
        HibernateOneToManyAssociation parentAssoc = 
            (HibernateOneToManyAssociation)newParent.getAssociation(type);
        HibernateOneToManyAssociation childAssoc = 
            (HibernateOneToManyAssociation)newChild.getAssociation(type);

        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            if (sameChild) {
                // REVIEW: 12/19/07: There's a non-unique multiplicity type
                // that probably needs to be handled here.
                assert(equals(getParent(), newParent));
                assert(getChildren().contains(newChild));
                return false;
            }

            if (childAssoc != null) {
                // Child associated with another parent.
                childAssoc.removeAll(newChild);
            }
            
            newChild.setAssociation(type, this);
            getChildren().add(newChild);
            return true;
        }

        // sameChild == true: childAssoc == this (modulo Hibernate magic)
        
        if (parentAssoc == null) {
            // Parent had no previous association.
            if (getParent() == null) {
                // child association is brand new, just set the parent
                newParent.setAssociation(type, this);
                setParent(newParent);
                return true;
            }
            
            // Child has an old parent, create a new association for the
            // parent.
            parentAssoc = 
                (HibernateOneToManyAssociation)
                newParent.getOrCreateAssociation(type);                
        }
        
        return parentAssoc.add(newParent, newChild);
    }

    @Override
    public void add(
        int index, 
        HibernateAssociable newParent,
        HibernateAssociable newChild)
    {
        final String type = getType();

        // This association must be related to one of the two objects.
        HibernateOneToManyAssociation parentAssoc = 
            (HibernateOneToManyAssociation)newParent.getAssociation(type);
        HibernateOneToManyAssociation childAssoc = 
            (HibernateOneToManyAssociation)newChild.getAssociation(type);

        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            if (sameChild) {
                // REVIEW: 12/19/07: There's a non-unique multiplicity type
                // that probably needs to be handled here.
                return;
            }

            if (childAssoc != null) {
                // Child associated with another parent.
                childAssoc.getChildren().remove(newChild);
                
                // REVIEW: 12/19/07: Should we delete childAssoc "if (childAssoc.getChildren().isEmpty())"?
            }
            
            newChild.setAssociation(type, this);
            getChildren().add(index, newChild);
            return;
        }

        if (parentAssoc == null) {
            // Parent had no previous association.
            newParent.setAssociation(type, this);
            setParent(newParent);
            return;
        }
        
        // Associating child with a new parent.  Invoke parent association's
        // add method.
        parentAssoc.add(index, newParent, newChild);
    }

    @Override
    public boolean remove(
        HibernateAssociable parent, HibernateAssociable child)
    {
        final String type = getType();
        
        // This association must be related to one of the two objects.
        HibernateOneToManyAssociation parentAssoc = 
            (HibernateOneToManyAssociation)parent.getAssociation(type);
        HibernateOneToManyAssociation childAssoc = 
            (HibernateOneToManyAssociation)child.getAssociation(type);
        
        if (!equals(parentAssoc, childAssoc)) {
            // Objects aren't association
            return false;
        }
        
        assert(parent.getAssociation(type).equals(this));
        assert(child.getAssociation(type).equals(this));
        assert(equals(getParent(), parent));
        assert(getChildren().contains(child));
        
        child.setAssociation(type, null);
        children.remove(child);
        
        if (children.isEmpty()) {
            parent.setAssociation(type, null);
            
            HibernateMDRepository.getCurrentSession().delete(this);
        }
        
        return true;
    }
    
    @Override
    public void removeAll(HibernateAssociable item)
    {
        if (!equals(item, getParent())) {
            assert(getChildren().contains(item));
            
            remove(getParent(), item);
            return;
        }
        
        while(!getChildren().isEmpty()) {
            HibernateAssociable child = getChildren().get(0);
            remove(item, child);
        }
    }
    
    @Override
    public void clear(HibernateAssociable item)
    {
        assert(equals(getParent(), item));
        
        removeAll(item);
    }
    
    @Override
    protected List<HibernateAssociable> get(HibernateAssociable item)
    {
        if (!equals(item, parent)) {
            return Collections.emptyList();
        }
        
        return children;
    }

    @Override
    public Iterator<RefAssociationLink> iterator()
    {
        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        HibernateAssociable parent = getParent();
        for(HibernateAssociable child: getChildren()) {
            RefAssociationLink link =
                new org.eigenbase.enki.jmi.impl.RefAssociationLink(
                    parent, child);
            links.add(link);
        }
        return links.iterator();
    }
}

// End HibernateOneToManyAssociation.java
