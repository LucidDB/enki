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
 * HibernateOneToOneAssociation extends HibernateAssociation and stores
 * one-to-one associations. 
 * 
 * @author Stephan Zuercher
 */
public class HibernateOneToOneAssociation
    extends HibernateAssociation
{
    private HibernateAssociable parent;
    private HibernateAssociable child;
    
    public HibernateOneToOneAssociation()
    {
    }
    
    public HibernateAssociable getParent()
    {
        return parent;
    }
    
    public void setParent(HibernateAssociable parent)
    {
        this.parent = parent;
    }
    
    public <E> E getParent(Class<E> cls)
    {
        return cls.cast(parent);
    }

    public HibernateAssociable getChild()
    {
        return child;
    }

    public void setChild(HibernateAssociable child)
    {
        this.child = child;
    }
    
    public <E> E getChild(Class<E> cls)
    {
        return cls.cast(child);
    }

    @Override
    public boolean add(
        HibernateAssociable newParent, HibernateAssociable newChild)
    {
        final String type = getType();
        
        HibernateOneToOneAssociation parentAssoc = 
            (HibernateOneToOneAssociation)newParent.getAssociation(
                type, true);
        HibernateOneToOneAssociation childAssoc = 
            (HibernateOneToOneAssociation)newChild.getAssociation(
                type, false);
                
        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        
        if (sameParent) {
            if (sameChild) {
                // Nothing to do.
                assert(this.equals(newParent.getAssociation(type, true)));
                assert(this.equals(newChild.getAssociation(type, false)));
                return false;
            }
            
            if (childAssoc != null) {
                // Child associated with another parent.
                childAssoc.setChild(null);
                
                // REVIEW: 12/19/07: Should we delete childAssoc?
            }
            
            newChild.setAssociation(type, false, this);
            setChild(newChild);
            return true;
        }

        if (parentAssoc == null) {
            if (getParent() != null) {
                getParent().setAssociation(type, true, null);
            }
            newParent.setAssociation(type, true, this);
            setParent(newParent);
            return true;
        }
        
        // Associating child with a new parent.  Invoke parent association's
        // add method.
        return parentAssoc.add(newParent, newChild);
    }

    @Override
    public void add(
        int index, HibernateAssociable parent, HibernateAssociable child)
    {
        add(parent, child);
    }
    
    @Override
    public boolean remove(
        HibernateAssociable parent, HibernateAssociable child)
    {
        final String type = getType();

        if (!equals(
                parent.getAssociation(type, true), 
                child.getAssociation(type, false)))
        {
            // Objects not associated.
            return false;
        }
        
        assert(equals(getParent(), parent) && equals(getChild(), child));
        assert(this.equals(parent.getAssociation(type, true)));
        assert(this.equals(child.getAssociation(type, false)));
        
        getParent().setAssociation(type, true, null);
        getChild().setAssociation(type, false, null);
        
        HibernateMDRepository.getCurrentSession().delete(this);
        
        return true;
    }
    
    @Override
    public void removeAll(HibernateAssociable item)
    {
        assert(equals(getParent(), item) || equals(getChild(), item));
        
        remove(getParent(), getChild());
    }

    @Override
    public void clear(HibernateAssociable item)
    {
        assert(equals(getParent(), item));
        
        remove(getParent(), getChild());
    }
    
    @Override
    public List<HibernateAssociable> get(HibernateAssociable item)
    {
        if (equals(getParent(), item)) {
            return Collections.singletonList(getChild());
        } else if (equals(getChild(), item)) {
            return Collections.singletonList(getParent());
        } else {
            return Collections.emptyList();
        }
    }
    
    @Override
    public Iterator<RefAssociationLink> iterator()
    {
        RefAssociationLink link = 
            new org.eigenbase.enki.jmi.impl.RefAssociationLink(
                getParent(), getChild());
        
        return Collections.singleton(link).iterator();
    }
}

// End HibernateOneToOneAssociation.java
