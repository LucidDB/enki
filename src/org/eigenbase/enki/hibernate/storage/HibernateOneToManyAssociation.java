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
import org.eigenbase.enki.jmi.impl.*;

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
    /** 
     * If true, this is a many-to-1 association.  That is, end1 is not the
     * single end.
     */
    private boolean reversed;
    private HibernateAssociable parent;
    private List<HibernateAssociable> children;
    
    public HibernateOneToManyAssociation()
    {
        this.children = new ArrayList<HibernateAssociable>();
    }
    
    public boolean getReversed()
    {
        return reversed;
    }
    
    public void setReversed(boolean reversed)
    {
        this.reversed = reversed;
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
        HibernateAssociable end1, HibernateAssociable end2)
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
        HibernateOneToManyAssociation parentAssoc = 
            (HibernateOneToManyAssociation)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyAssociation childAssoc = 
            (HibernateOneToManyAssociation)newChild.getAssociation(
                type, childIsFirstEnd);

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
            
            newChild.setAssociation(type, childIsFirstEnd, this);
            getChildren().add(newChild);
            return true;
        }

        // sameChild == true: childAssoc == this (modulo Hibernate magic)
        
        if (parentAssoc == null) {
            // Parent had no previous association.
            if (getParent() == null) {
                // child association is brand new, just set the parent
                newParent.setAssociation(type, parentIsFirstEnd, this);
                setParent(newParent);
                return true;
            }
            
            // Child has an old parent, create a new association for the
            // parent.
            parentAssoc = 
                (HibernateOneToManyAssociation)
                newParent.getOrCreateAssociation(type, parentIsFirstEnd);                
        }
        
        // REVIEW: SWZ: 1/9/08: Could convert to an addInternal model rather
        // than using end1/end2 here.  We currently do this so that the
        // recursive call doesn't mistakenly re-switch the ends. 
        return parentAssoc.add(end1, end2);
    }

    @Override
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
        HibernateOneToManyAssociation parentAssoc = 
            (HibernateOneToManyAssociation)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyAssociation childAssoc = 
            (HibernateOneToManyAssociation)newChild.getAssociation(
                type, childIsFirstEnd);

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

    @Override
    public boolean remove(
        HibernateAssociable end1, HibernateAssociable end2)
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

        return removeInternal(parent, child);
    }
    
    private boolean removeInternal(
        HibernateAssociable parent, HibernateAssociable child)
    {   
        final String type = getType();

        boolean childIsFirstEnd = getReversed();
        boolean parentIsFirstEnd = !childIsFirstEnd;

        // This association must be related to one of the two objects.
        HibernateOneToManyAssociation parentAssoc = 
            (HibernateOneToManyAssociation)parent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyAssociation childAssoc = 
            (HibernateOneToManyAssociation)child.getAssociation(
                type, childIsFirstEnd);
        
        if (!equals(parentAssoc, childAssoc)) {
            // Objects aren't association
            return false;
        }
        
        List<HibernateAssociable> children = getChildren();

        assert(parentAssoc.equals(this));
        assert(childAssoc.equals(this));
        assert(equals(getParent(), parent));
        assert(children.contains(child));
        
        child.setAssociation(type, childIsFirstEnd, null);
        children.remove(child);
        
        if (children.isEmpty()) {
            parent.setAssociation(type, parentIsFirstEnd, null);
            
            HibernateMDRepository.getCurrentSession().delete(this);
        }
        
        return true;
    }
    
    @Override
    public void removeAll(HibernateAssociable item)
    {
        HibernateAssociable parent = getParent();
        List<HibernateAssociable> children = getChildren();
        
        if (!equals(item, parent)) {
            assert(children.contains(item));
            
            // Null parent occurs when child is first end of association and
            // is added (via refAddLink) to a parent that already had a child.
            // Just ignore the call and the unused association should be gc'd.
            if (parent != null) {
                removeInternal(parent, item);
            }
            return;
        }
        
        while(!children.isEmpty()) {
            HibernateAssociable child = children.get(0);
            removeInternal(item, child);
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
        if (!equals(item, getParent())) {
            return Collections.emptyList();
        }
        
        return getChildren();
    }

    @Override
    public Collection<RefAssociationLink> getLinks()
    {
        boolean reversed = getReversed();
        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        HibernateAssociable parent = getParent();
        for(HibernateAssociable child: getChildren()) {
            RefAssociationLink link;
            if (reversed) {
                link = new RefAssociationLinkImpl(child, parent);
            } else {
                link = new RefAssociationLinkImpl(parent, child);
            }
            links.add(link);
        }
        return links;
    }

    @Override
    public Collection<? extends RefObject> query(boolean returnSecondEnd)
    {
        boolean returnParent = (returnSecondEnd == getReversed());

        if (returnParent) {
            return Collections.singleton(getParent());
        } else {
            return Collections.unmodifiableCollection(getChildren());
        }
    }
}

// End HibernateOneToManyAssociation.java
