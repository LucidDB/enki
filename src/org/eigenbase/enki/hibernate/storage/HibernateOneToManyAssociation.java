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

        boolean sameParent = equals(parent, newParent);
        if (sameParent && getChildren().contains(newChild)) {
            // Nothing to do here.
            assert(newParent.getAssociation(type).equals(this));
            assert(newChild.getAssociation(type).equals(this));
            return false;
        }
        
        // Remove child from previous association, if any. (FYI, if not same
        // parent, childAssoc == this).
        HibernateOneToManyAssociation childAssoc =
            (HibernateOneToManyAssociation)newChild.getAssociation(type);
        if (childAssoc != null) {
            assert(sameParent || childAssoc.equals(this));
            childAssoc.remove(childAssoc.getParent(), newChild);
        }

        if (sameParent) {
            assert(newParent.getAssociation(type).equals(this));

            // Add child to this association.
            newChild.setAssociation(type, this);
            getChildren().add(newChild);
        } else {
            HibernateOneToManyAssociation parentAssoc =
                (HibernateOneToManyAssociation)newParent.getAssociation(type);
            if (parentAssoc == null) {
                parentAssoc = new HibernateOneToManyAssociation();
                parentAssoc.setType(type);
                parentAssoc.setParent(newParent);
                newParent.setAssociation(type, parentAssoc);
            }
            
            newChild.setAssociation(type, parentAssoc);
            parentAssoc.getChildren().add(newChild);
            
            // TODO: delete this association if there are no children left?
        }
        
        return true;
    }

    public void add(
        int index, 
        HibernateAssociable left,
        HibernateAssociable right)
    {
        assert(equals(left, parent) || equals(right, parent));

        final String type = getType();
        
        HibernateAssociable child = left;
        if (equals(left, parent)) {
            child = right;
        }
        
        assert(parent.getAssociation(type).equals(this));
        
        // Remove child from previous association, if any.
        HibernateOneToManyAssociation childAssoc =
            (HibernateOneToManyAssociation)child.getAssociation(type);
        if (childAssoc != null) {
            childAssoc.remove(childAssoc.getParent(), right);
        }
        
        child.setAssociation(type, this);
        children.add(index, child);
    }

    @Override
    public boolean remove(HibernateAssociable left, HibernateAssociable right)
    {
        assert(equals(left, parent) || equals(right, parent));

        final String type = getType();
        
        HibernateAssociable child = left;
        if (equals(left, parent)) {
            child = right;
        }
        
        assert(parent.getAssociation(type).equals(this));
        assert(child.getAssociation(type).equals(this));
        assert(children.contains(child));
        
        child.setAssociation(type, null);
        boolean result = children.remove(child);
        
        if (children.isEmpty()) {
            parent.setAssociation(type, null);
            
            // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete this?
            // Or does that happen auto-magically?
        }
        
        return result;
    }
    
    @Override
    public void clear(HibernateAssociable item)
    {
        final String type = getType();
        
        assert(equals(item, parent));
        
        for(HibernateAssociable child: children) {
            child.setAssociation(type, null);            
        }
        children.clear();
        
        parent.setAssociation(type, null);
            
        // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete this?
        // Or does that happen auto-magically?
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
