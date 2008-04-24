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

import javax.jmi.reflect.*;

import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateOneToManyAssociationBase is an abstract base class for one-to-many
 * association storage.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyAssociationBase
    extends HibernateAssociationBase
{
    /** 
     * If true, this is a many-to-1 association.  That is, end1 is not the
     * single end.
     */
    private boolean reversed;
    private HibernateAssociable parent;
    
    public boolean getReversed()
    {
        return reversed;
    }
    
    public void setReversed(boolean reversed)
    {
        this.reversed = reversed;
    }
    
    public abstract boolean getUnique();
    
    public HibernateAssociable getParent()
    {
        return parent;
    }

    public void setParent(HibernateAssociable parent)
    {
        this.parent = parent;
    }
    
    protected abstract Collection<HibernateAssociable> getCollection();

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
        HibernateOneToManyAssociationBase parentAssoc = 
            (HibernateOneToManyAssociationBase)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyAssociationBase childAssoc = 
            (HibernateOneToManyAssociationBase)newChild.getAssociation(
                type, childIsFirstEnd);

        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            if (sameChild && getUnique()) {
                assert(equals(getParent(), newParent));
                assert(getCollection().contains(newChild));
                return false;
            }

            if (childAssoc != null && !sameChild) {
                // Child associated with another parent.
                childAssoc.removeAll(newChild, false);
            }
            
            newChild.setAssociation(type, childIsFirstEnd, this);
            getCollection().add(newChild);
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
                (HibernateOneToManyAssociationBase)
                newParent.getOrCreateAssociation(type, parentIsFirstEnd);                
        }
        
        // REVIEW: SWZ: 1/9/08: Could convert to an addInternal model rather
        // than using end1/end2 here.  We currently do this so that the
        // recursive call doesn't mistakenly re-switch the ends. 
        return parentAssoc.add(end1, end2);
    }

    public boolean remove(HibernateAssociable end1, HibernateAssociable end2)
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

        return removeInternal(parent, child, -1);
    }
    
    protected boolean removeInternal(
        HibernateAssociable parent, HibernateAssociable child, int index)
    {   
        final String type = getType();

        boolean childIsFirstEnd = getReversed();
        boolean parentIsFirstEnd = !childIsFirstEnd;

        // This association must be related to one of the two objects.
        HibernateOneToManyAssociationBase parentAssoc = 
            (HibernateOneToManyAssociationBase)parent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyAssociationBase childAssoc = 
            (HibernateOneToManyAssociationBase)child.getAssociation(
                type, childIsFirstEnd);
        
        if (!equals(parentAssoc, childAssoc)) {
            // Objects aren't associated
            return false;
        }
        
        Collection<HibernateAssociable> children = getCollection();

        assert(parentAssoc.equals(this));
        assert(childAssoc.equals(this));
        assert(equals(getParent(), parent));
        
        int count = getUnique() ? 1 : count(child, children);
        assert(count >= 1);
        
        if (count == 1) {
            child.setAssociation(type, childIsFirstEnd, null);
        }
        
        if (index == -1) {
            children.remove(child);
        } else {
            HibernateAssociable removed = 
                ((List<HibernateAssociable>)children).remove(index);
            assert(child.equals(removed));
        }
        
        if (children.isEmpty()) {
            parent.setAssociation(type, parentIsFirstEnd, null);
            
            delete(getHibernateRepository(parent));
        }
        
        return true;
    }
    
    
    public void postRemove(HibernateAssociable end1, HibernateAssociable end2)
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

        final String type = getType();

        boolean childIsFirstEnd = getReversed();
        boolean parentIsFirstEnd = !childIsFirstEnd;

        // This association must be related to one of the two objects.
        HibernateOneToManyAssociationBase parentAssoc = 
            (HibernateOneToManyAssociationBase)parent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyAssociationBase childAssoc = 
            (HibernateOneToManyAssociationBase)child.getAssociation(
                type, childIsFirstEnd);
        
        if (!equals(parentAssoc, childAssoc)) {
            // Objects aren't associated
            assert(false);
            return;
        }
        
        Collection<HibernateAssociable> children = getCollection();

        assert(parentAssoc.equals(this));
        assert(childAssoc.equals(this));
        assert(equals(getParent(), parent));
        
        int count = getUnique() ? 1 : count(child, children);
        assert(count >= 1);
        
        if (count == 1) {
            child.setAssociation(type, childIsFirstEnd, null);
        }
        
        assert(!children.contains(child));
        
        if (children.isEmpty()) {
            parent.setAssociation(type, parentIsFirstEnd, null);
            
            delete(getHibernateRepository(parent));
        }
    }
    
    protected int count(
        HibernateAssociable item, Collection<HibernateAssociable> items)
    {
        int count = 0;
        for(HibernateAssociable a: items) {
            if (a.equals(item)) {
                count++;
            }
        }
        return count;
    }
    
    public void removeAll(HibernateAssociable item, boolean cascadeDelete)
    {
        HibernateAssociable parent = getParent();
        Collection<HibernateAssociable> children = getCollection();
        
        if (!equals(item, parent)) {
            // REVIEW: SWZ: 4/22/08: This causes a lot of unnecessary Hibernate
            // activity.
//            assert(children.contains(item));
            
            // Null parent occurs when child is first end of association and
            // is added (via refAddLink) to a parent that already had a child.
            // Just ignore the call and the unused association should be gc'd.
            if (parent != null) {
                removeInternal(parent, item, -1);
                
                if (cascadeDelete) {
                    parent.refDelete();
                }
            }
            return;
        }
        
        while(!children.isEmpty()) {
            HibernateAssociable child = children.iterator().next();
            removeInternal(item, child, -1);
            
            if (cascadeDelete) {
                child.refDelete();
            }
        }
    }
    
    public void clear(HibernateAssociable item)
    {
        assert(equals(getParent(), item));
        
        removeAll(item, false);
    }

    public Collection<RefAssociationLink> getLinks()
    {
        boolean reversed = getReversed();
        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        HibernateAssociable parent = getParent();
        for(HibernateAssociable child: getCollection()) {
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

    public Collection<? extends RefObject> query(boolean returnSecondEnd)
    {
        boolean returnParent = (returnSecondEnd == getReversed());

        if (returnParent) {
            RefObject parent = getParent();
            if (parent != null) {
                return Collections.singleton(getParent());
            } else {
                return Collections.emptySet();
            }
        } else {
            return Collections.unmodifiableCollection(getCollection());
        }
    }
}

// End HibernateOneToManyAssociationBase.java
