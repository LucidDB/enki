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
 * HibernateOneToManyLazyAssociation represents an association between two
 * metamodel types with especially lazy loading semantics.  The association
 * to metamodel instances is not known to Hibernate, which allows us to 
 * avoid loading instance objects except when truly necessary.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyLazyAssociation
    extends HibernateOneToManyLazyAssociationBase
{
    private Collection<Element> children;
    
    public HibernateOneToManyLazyAssociation()
    {
        super();
     
        this.children = new HashSet<Element>();
    }
    
    public Collection<Element> getChildren()
    {
        return children;
    }
    
    public void setChildren(Collection<Element> children)
    {
        this.children = children;
    }
    
    public void addInitialChild(HibernateAssociable child)
    {
        Collection<Element> children = getChildren();
        
        if (children == null) {
            children = new HashSet<Element>();
            setChildren(children);
        }
        
        children.add(newElement(child));
    }
    
    @Override
    public Collection<Element> getElements()
    {
        return getChildren();
    }
    
    @Override
    protected void emptyElements()
    {
        setChildren(new HashSet<Element>());
    }

    @Override
    protected boolean getUnique()
    {
        return true;
    }

    public Collection<HibernateAssociable> getCollection()
    {
        return new ElementCollection(getChildren());
    }
    
    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        HibernateRefObject refObj = (HibernateRefObject)item;
        
        if (refObj.getMofId() == getParentId()) {
            return getCollection();
        } else {
            return Collections.singleton(getParent());
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

        boolean inPreviewDelete = 
            getHibernateRepository(item).inPreviewDelete();

        Collection<Element> children = getChildren();
        
        if (isParent) {
            if (!inPreviewDelete) {
                item.setAssociation(type, parentIsFirstEnd, null);
            }
            
            for(Element child: children) {
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
        
                children.remove(elem);
            
                if (children.isEmpty()) {
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
        return Kind.ONE_TO_MANY;
    }
}

// End HibernateOneToManyLazyAssociation.java
