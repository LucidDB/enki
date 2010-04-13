/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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

import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.jmi.*;

/**
 * HibernateOnetoManyLazyAssociationBase is base class for one-to-many 
 * association storage.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyLazyAssociationBase
    extends HibernateLazyAssociationBase
{
    /** 
     * If true, this is a many-to-1 association.  That is, end1 is not the
     * single end.
     */
    private boolean reversed;
    private String parentType;
    private long parentId;

    public boolean getReversed()
    {
        return reversed;
    }

    public void setReversed(boolean reversed)
    {
        this.reversed = reversed;
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

    public HibernateAssociable getParent()
    {
        String type = getParentType();
        if (type == null) {
            return null;
        }
        
        return (HibernateAssociable)load(type, getParentId());
    }

    protected abstract boolean getUnique();
    
    public abstract Collection<Element> getElements();
    protected abstract void emptyElements();
    public abstract Collection<HibernateAssociable> getCollection();
    
    public abstract void addInitialChild(HibernateAssociable child);

    public void setInitialParent(HibernateAssociable parent)
    {
        HibernateRefObject refObj = (HibernateRefObject)parent;
        setParentType(refObj.getClassIdentifier());
        setParentId(refObj.getMofId());
    }
    
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
        HibernateOneToManyLazyAssociationBase parentAssoc = 
            (HibernateOneToManyLazyAssociationBase)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyLazyAssociationBase childAssoc = 
            (HibernateOneToManyLazyAssociationBase)newChild.getAssociation(
                type, childIsFirstEnd);

        return addInternal(
            type, 
            parentAssoc, 
            newParent, 
            childAssoc, 
            newChild, 
            parentIsFirstEnd);
    }
    
    private boolean addInternal(
        final String type,
        HibernateOneToManyLazyAssociationBase parentAssoc,
        HibernateAssociable newParent,
        HibernateOneToManyLazyAssociationBase childAssoc,
        HibernateAssociable newChild,
        boolean parentIsFirstEnd)
    {
        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            if (sameChild && getUnique()) {
                assert(
                    equals(
                        getParentId(), 
                        ((HibernateRefObject)newParent).getMofId()));
                return false;
            }

            Element elem = newElement(newChild);

            if (childAssoc != null && !sameChild) {
                // Child associated with another parent.
                boolean delete = false;
                if (getUnique()) {
                    childAssoc.getElements().remove(elem);
                    delete = childAssoc.getElements().isEmpty();
                } else {
                    Collection<HibernateAssociable> c = 
                        childAssoc.getCollection();
                    while(c.remove(newChild));
                    delete = c.isEmpty();
                }
                
                if (delete) {
                    HibernateAssociable childsParent = childAssoc.getParent();
                    if (childsParent != null) {
                        childsParent.setAssociation(
                            type, parentIsFirstEnd, null);
                    }
                    childAssoc.delete(getHibernateRepository(newChild));
                }
            }
            
            newChild.setAssociation(type, !parentIsFirstEnd, this);
            getCollection().add(newChild);
            return true;
        }

        // sameChild == true: childAssoc == this (modulo Hibernate magic)
        
        if (parentAssoc == null) {
            // Parent had no previous association.
            if (getParentType() == null) {
                // child association is brand new, just set the parent
                newParent.setAssociation(type, parentIsFirstEnd, this);
                HibernateRefObject refObj = (HibernateRefObject)newParent;
                setParentType(refObj.getClassIdentifier());
                setParentId(refObj.getMofId());
                return true;
            }
            
            // Child has an old parent, create a new association for the
            // parent.
            parentAssoc = 
                (HibernateOneToManyLazyAssociationBase)
                newParent.getOrCreateAssociation(type, parentIsFirstEnd);                
        }
        
        return parentAssoc.addInternal(
            type,
            parentAssoc,
            newParent,
            childAssoc,
            newChild,
            parentIsFirstEnd);
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

        boolean parentIsFirstEnd = !getReversed();

        if (((HibernateRefObject)parent).getMofId() == getParentId()) {
            Element childElement = newElement(child);
            int count = getUnique() ? 1 : count(childElement);

            if (count == 1) {
                // Removing only instance of this object in the collection
                // (either due to uniqueness of collection or because the
                // element is actually unique in a list)
                child.setAssociation(type, !parentIsFirstEnd, null);
            }
            
            Collection<HibernateAssociable> children = getCollection();

            boolean result;
            if (index == -1) {
                result = children.remove(child);
            } else {
                HibernateAssociable removed = 
                    ((List<HibernateAssociable>)children).remove(index);
                assert(
                    ((HibernateRefObject)removed).getMofId() == 
                        childElement.getChildId().longValue());
                result = true;
            }
            
            if (children.isEmpty()) {
                parent.setAssociation(type, parentIsFirstEnd, null);

                emptyElements();
                
                delete(getHibernateRepository(parent));
            }
            
            return result;
        }
        
        return false;
    }

    private int count(Element child)
    {
        int count = 0;
        for(Element e: getElements()) {
            if (child.equals(e)) {
                count++;
            }
        }

        return count;
    }
    
    public String getCollectionName()
    {
        return HibernateMappingHandler.ASSOC_ONE_TO_MANY_CHILDREN_PROPERTY;
    }
}

// End HibernateOneToManyLazyAssociationBase.java
