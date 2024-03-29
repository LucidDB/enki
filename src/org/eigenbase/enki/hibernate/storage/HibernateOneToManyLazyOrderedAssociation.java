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

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateOneToManyLazyOrderedAssociation represents an association between 
 * two metamodel types with especially lazy loading semantics.  The association
 * to metamodel instances is not known to Hibernate, which allows us to 
 * avoid loading instance objects except when truly necessary.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyLazyOrderedAssociation
    extends HibernateOneToManyLazyAssociationBase
    implements HibernateOrderedAssociation
{
    private List<Element> children;
    private ElementList childrenWrapper;
    
    public HibernateOneToManyLazyOrderedAssociation()
    {
        super();
     
        this.children = new ArrayList<Element>();
    }
    
    public List<Element> getChildren()
    {
        return children;
    }
    
    @Override
    public void addInitialChild(HibernateAssociable child)
    {
        List<Element> childList = getChildren();
        
        if (childList == null) {
            childList = new ArrayList<Element>();
            setChildren(childList);
        }
        
        childList.add(newElement(child));
    }
    
    @Override
    public Collection<Element> getElements()
    {
        return getChildren();
    }

    @Override
    protected void emptyElements()
    {
        getChildren().clear();
    }

    @Override
    protected boolean getUnique()
    {
        return false;
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
        HibernateOneToManyLazyOrderedAssociation parentAssoc = 
            (HibernateOneToManyLazyOrderedAssociation)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyLazyOrderedAssociation childAssoc = 
            (HibernateOneToManyLazyOrderedAssociation)newChild.getAssociation(
                type, childIsFirstEnd);

        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            Element elem = newElement(newChild);

            if (childAssoc != null && !sameChild) {
                while(childAssoc.getElements().remove(elem));
                childAssoc.childrenWrapper = null;
                if (childAssoc.getElements().isEmpty()) {
                    HibernateAssociable childsParent = childAssoc.getParent();
                    if (childsParent != null) {
                        childsParent.setAssociation(
                            type, parentIsFirstEnd, null);
                    }
                    childAssoc.delete(getHibernateRepository(newChild));
                }
            }
            
            newChild.setAssociation(type, childIsFirstEnd, this);
            getCollection().add(index, newChild);
            return;
        }

        if (parentAssoc == null) {
            // Parent had no previous association.
            newParent.setAssociation(type, parentIsFirstEnd, this);
            HibernateRefObject refObj = (HibernateRefObject)newParent;
            setParentType(refObj.getClassIdentifier());
            setParentId(refObj.getMofId());
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

    public void setChildren(List<Element> children)
    {
        this.children = children;
    }
    
    public List<HibernateAssociable> getCollection()
    {
        List<Element> childList = getChildren();
        
        if (childrenWrapper == null || childrenWrapper.elements != childList) {
            childrenWrapper = new ElementList(childList);
        }

        return childrenWrapper;
    }
    
    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        return getOrdered(item);
    }
    
    public List<HibernateAssociable> getOrdered(HibernateAssociable item)
    {
        HibernateRefObject refObj = (HibernateRefObject)item;
        
        if (refObj.getMofId() == getParentId()) {
            return getCollection();
        } else {
            return Collections.singletonList(getParent());
        }
    }
    
    public void clear(HibernateAssociable item)
    {
        assert(equals(getParent(), item));
        
        removeAll(item, !getReversed(), false);
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

    public void postRemove(HibernateAssociable end1, HibernateAssociable end2)
    {
        HibernateAssociable parent;
        HibernateAssociable child;
        boolean parentIsFirstEnd;
        if (getReversed()) {
            parent = end2;
            child = end1;
            parentIsFirstEnd = false;
        } else {
            parent = end1;
            child = end2;
            parentIsFirstEnd = true;            
        }
        
        Element elem = newElement(child);
        
        if (getChildren().contains(elem)) {
            // Children contained a second copy before remove, nothing else 
            // left to do.
            return;
        }
        
        child.setAssociation(getType(), !parentIsFirstEnd, null);
        
        if (getChildren().isEmpty()) {
            parent.setAssociation(getType(), parentIsFirstEnd, null);
            
            emptyElements();
            
            delete(getHibernateRepository(parent));
        }
    }

    public Collection<? extends RefObject> query(boolean returnSecondEnd)
    {
        boolean returnManyEnd = (returnSecondEnd != getReversed());
        
        if (returnManyEnd) {
            return getCollection();
        } else if (getParentType() != null) {
            return Collections.singleton(getParent());
        } else {
            return Collections.emptySet();
        }
    }

    public void removeAll(
        HibernateAssociable item,
        boolean isFirstEnd,
        boolean cascadeDelete)
    {
        final String type = getType();
        
        boolean isParent;
        boolean parentIsFirstEnd;
        if (getReversed()) {
            isParent = !isFirstEnd;
            parentIsFirstEnd = false;
        } else {
            isParent = isFirstEnd;
            parentIsFirstEnd = true;
        }

        List<Element> childList = getChildren();

        boolean inPreviewDelete =
            getHibernateRepository(item).inPreviewDelete();
        
        if (isParent) {
            if (!inPreviewDelete) {
                item.setAssociation(type, parentIsFirstEnd, null);
            }
            
            for(Element child: childList) {
                RefObject childRefObj = load(child);
                
                if (!inPreviewDelete) {
                    ((HibernateAssociable)childRefObj).setAssociation(
                        type, !parentIsFirstEnd, null);
                }
                
                if (cascadeDelete) {
                    childRefObj.refDelete();
                }
            }
            
            if (!inPreviewDelete) {
                emptyElements();
                delete(getHibernateRepository(item));
            }
        } else {
            if (!inPreviewDelete) {
                item.setAssociation(type, !parentIsFirstEnd, null);
                Element elem = newElement(item);
                childList.remove(elem);
            
                if (childList.isEmpty()) {
                    getParent().setAssociation(type, parentIsFirstEnd, null);
                    emptyElements();
                    delete(getHibernateRepository(item));
                }
            }
            
            HibernateAssociable parent = getParent();
            if (cascadeDelete && parent != null) {
                parent.refDelete();
            }            
        }
    }
    
    public final Kind getKind()
    {
        return Kind.ONE_TO_MANY_ORDERED;
    }
}

// End HibernateOneToManyLazyOrderedAssociation.java
