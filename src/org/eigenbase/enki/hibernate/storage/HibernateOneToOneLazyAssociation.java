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

import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateOneToOneLazyAssociation extends HibernateAssociation to provide a 
 * base class that stores lazy one-to-one associations.  It is extended 
 * per-model to provide separate storage for each model's associations.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToOneLazyAssociation
    extends HibernateLazyAssociationBase
{
    private String parentType;
    private long parentId;
    private String childType;
    private long childId;
    
    public HibernateOneToOneLazyAssociation()
    {
    }
    
    public String getParentType()
    {
        return parentType;
    }
    
    public void setParentType(String parentType)
    {
        this.parentType = parentType;
    }
    
    public long getParentId()
    {
        return parentId;
    }
    
    public void setParentId(long parentId)
    {
        this.parentId = parentId;
    }
    
    public String getChildType()
    {
        return childType;
    }
    
    public void setChildType(String childType)
    {
        this.childType = childType;
    }
    
    public long getChildId()
    {
        return childId;
    }
    
    public void setChildId(long childId)
    {
        this.childId = childId;
    }
    
    public void setInitialParent(HibernateAssociable parent)
    {
        HibernateRefObject refObj = (HibernateRefObject)parent;
        setParentType(refObj.getClassIdentifier());
        setParentId(refObj.getMofId());
    }
    
    public void setInitialChild(HibernateAssociable child)
    {
        HibernateRefObject refObj = (HibernateRefObject)child;
        setChildType(refObj.getClassIdentifier());
        setChildId(refObj.getMofId());
    }

    public HibernateAssociable getParent()
    {
        String parentType = getParentType();
        if (parentType == null) {
            return null;
        }
        
        return (HibernateAssociable)load(parentType, getParentId());
    }
    
    public <E> E getParent(Class<E> cls)
    {
        return cls.cast(getParent());
    }

    public HibernateAssociable getChild()
    {
        String childType = getChildType();
        if (childType == null) {
            return null;
        }
        
        return (HibernateAssociable)load(childType, getChildId());
    }
    
    public <E> E getChild(Class<E> cls)
    {
        return cls.cast(getChild());
    }

    public boolean add(
        HibernateAssociable newParent, HibernateAssociable newChild)
    {
        final String type = getType();
        
        HibernateOneToOneLazyAssociation parentAssoc = 
            (HibernateOneToOneLazyAssociation)newParent.getAssociation(
                type, true);
        HibernateOneToOneLazyAssociation childAssoc = 
            (HibernateOneToOneLazyAssociation)newChild.getAssociation(
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
                // Child was associated with another parent.
                childAssoc.delete(getHibernateRepository());
            }
            
            newChild.setAssociation(type, false, this);

            HibernateRefObject newChildRefObj = (HibernateRefObject)newChild;
            setChildType(newChildRefObj.getClassIdentifier());
            setChildId(newChildRefObj.getMofId());
            return true;
        }

        if (parentAssoc == null) {
            HibernateAssociable parent = getParent();
            if (parent != null) {
                parent.setAssociation(type, true, null);
            }
            newParent.setAssociation(type, true, this);
            
            HibernateRefObject newParentRefObj = (HibernateRefObject)newParent;
            setParentType(newParentRefObj.getClassIdentifier());
            setParentId(newParentRefObj.getMofId());
            return true;
        }
        
        // Associating child with a new parent.  Invoke parent association's
        // add method.
        return parentAssoc.add(newParent, newChild);
    }

    public void add(
        int index, HibernateAssociable parent, HibernateAssociable child)
    {
        add(parent, child);
    }
    
    public boolean remove(
        HibernateAssociable parent, HibernateAssociable child)
    {
        final String type = getType();

        HibernateAssociation parentAssoc = parent.getAssociation(type, true);
        HibernateAssociation childAssoc = child.getAssociation(type, false);
        
        if (!equals(parentAssoc, childAssoc))
        {
            // Objects not associated.
            return false;
        }
        
        HibernateRefObject parentRefObj = (HibernateRefObject)parent;
        HibernateRefObject childRefObj = (HibernateRefObject)child;
        
        assert(parentRefObj.getClassIdentifier().equals(getParentType()));
        assert(childRefObj.getClassIdentifier().equals(getChildType()));
        assert(parentRefObj.getMofId() == getParentId());
        assert(childRefObj.getMofId() == getChildId());        
        assert(equals(this, parentAssoc));
        assert(equals(this, childAssoc));
        
        parent.setAssociation(type, true, null);
        child.setAssociation(type, false, null);
        
        delete(getHibernateRepository(parent));
        
        return true;
    }

    public boolean remove(
        int index, HibernateAssociable parent, HibernateAssociable child)
    {
        return remove(parent, child);
    }

    public void removeAll(
        HibernateAssociable item, boolean isFirstEnd, boolean cascadeDelete)
    {
        HibernateAssociable otherEnd = isFirstEnd ? getChild() : getParent();
        
        String type = getType();

        item.setAssociation(type, isFirstEnd, null);
        otherEnd.setAssociation(type, !isFirstEnd, null);
        
        delete(getHibernateRepository(item));
        
        if (cascadeDelete) {
            otherEnd.refDelete();
        }
    }

    public void postRemove(HibernateAssociable end1, HibernateAssociable end2)
    {
        final String type = getType();

        end1.setAssociation(type, true, null);
        end2.setAssociation(type, false, null);

        delete(getHibernateRepository(end1));
    }
    
    public void clear(HibernateAssociable item)
    {
        HibernateRefObject itemRefObj = (HibernateRefObject)item;
        assert(itemRefObj.getClassIdentifier().equals(getParentType()));
        assert(itemRefObj.getMofId() == getParentId());
        
        remove(item, getChild());
    }
    
    public List<HibernateAssociable> getOrdered(HibernateAssociable item)
    {
        long itemMofId = ((HibernateRefObject)item).getMofId();
        
        if (itemMofId == getParentId()) {
            return new ModifiableSingletonList(getChild());
        } else if (itemMofId == getChildId()) {
            return new ModifiableSingletonList(getParent());
        } else {
            return Collections.emptyList();
        }
    }
    
    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        return getOrdered(item);
    }
    
    public Collection<RefAssociationLink> getLinks()
    {
        RefAssociationLink link = 
            new RefAssociationLinkImpl(getParent(), getChild());
        
        return Collections.singleton(link);
    }

    public Collection<? extends RefObject> query(boolean returnSecondEnd)
    {
        RefObject result;
        if (returnSecondEnd) {
            result = getChild();
        } else {
            result = getParent();
        }
        
        if (result == null) {
            return Collections.emptySet();
        }
        
        return Collections.singleton(result);
    }
    
    private static class ModifiableSingletonList
        extends AbstractList<HibernateAssociable>
        implements List<HibernateAssociable>
    {
        private HibernateAssociable item;
        private int size;
        
        private ModifiableSingletonList(HibernateAssociable item)
        {
            this.item = item;
            this.size = 1;
        }
        
        @Override
        public HibernateAssociable get(int index)
        {
            if (index != 0 || size == 0) {
                throw new IndexOutOfBoundsException();
            }
            
            return item;
        }

        @Override
        public int size()
        {
            return size;
        }

        @Override
        public void add(int index, HibernateAssociable element)
        {
            if (size == 0 && index == 0) {
                item = element;
                return;
            }
            
            throw new IndexOutOfBoundsException();
        }

        @Override
        public HibernateAssociable set(int index, HibernateAssociable element)
        {
            if (size == 1 && index == 0) {
                HibernateAssociable result = item;
                item = element;
                return result;
            }
            
            throw new IndexOutOfBoundsException();
        }

        @Override
        public HibernateAssociable remove(int index)
        {
            if (size == 1 && index == 0) {
                HibernateAssociable result = item;
                item = null;
                size = 0;
                return result;
            }
            
            throw new IndexOutOfBoundsException();
        }
    }
    
    public Kind getKind()
    {
        return Kind.ONE_TO_ONE;
    }
    
    public String getCollectionName()
    {
        return null;
    }
}

// End HibernateOneToOneLazyAssociation.java
