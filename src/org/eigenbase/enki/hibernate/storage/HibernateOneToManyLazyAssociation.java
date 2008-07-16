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
    
    public void setInitialParent(HibernateAssociable parent)
    {
        HibernateRefObject refObj = (HibernateRefObject)parent;
        setParentType(refObj.getClassIdentifier());
        setParentId(refObj.getMofId());
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
    protected Collection<Element> getElements()
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

    public void setChildren(Collection<Element> children)
    {
        this.children = children;
    }
    
    public Collection<HibernateAssociable> getCollection()
    {
        return new ElementCollection(getChildren());
    }
    
    /*
    public boolean add(HibernateAssociable end1, HibernateAssociable end2)
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
        HibernateOneToManyLazyAssociation parentAssoc = 
            (HibernateOneToManyLazyAssociation)newParent.getAssociation(
                type, parentIsFirstEnd);
        HibernateOneToManyLazyAssociation childAssoc = 
            (HibernateOneToManyLazyAssociation)newChild.getAssociation(
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
        HibernateOneToManyLazyAssociation parentAssoc,
        HibernateAssociable newParent,
        HibernateOneToManyLazyAssociation childAssoc,
        HibernateAssociable newChild,
        boolean parentIsFirstEnd)
    {
        boolean sameParent = parentAssoc != null && parentAssoc.equals(this);
        boolean sameChild = childAssoc != null && childAssoc.equals(this);
        assert(sameParent || sameChild);
        
        if (sameParent) {
            if (sameChild) {
                assert(
                    equals(getParentId(), 
                           ((HibernateRefObject)newParent).getMofId()));
                return false;
            }

            Element elem = newElement(newChild);

            if (childAssoc != null && !sameChild) {
                // Child associated with another parent.
                childAssoc.getChildren().remove(elem);
                if (childAssoc.getChildren().isEmpty()) {
                    HibernateAssociable childsParent = childAssoc.getParent();
                    if (childsParent != null) {
                        childsParent.setAssociation(
                            type, parentIsFirstEnd, null);
                    }
                    childAssoc.delete(getHibernateRepository(newChild));
                }
            }
            
            newChild.setAssociation(type, !parentIsFirstEnd, parentAssoc);
            getChildren().add(elem);
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
                (HibernateOneToManyLazyAssociation)
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
*/
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

    /*
    public boolean remove(HibernateAssociable end1, HibernateAssociable end2)
    {
        final String type = getType();
        
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
        
        if (((HibernateRefObject)parent).getMofId() == getParentId()) {
            child.setAssociation(type, !parentIsFirstEnd, null);
            
            Collection<Element> children = getChildren();
            boolean result = children.remove(newElement(child));
            
            if (children.isEmpty()) {
                parent.setAssociation(type, parentIsFirstEnd, null);
                delete(getHibernateRepository(parent));
            }
            
            return result;
        }
        
        return false;
    }
*/
    
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

        Collection<Element> children = getChildren();
        
        if (isParent) {
            item.setAssociation(type, parentIsFirstEnd, null);
            
            for(Element child: children) {
                RefObject childRefObj = load(child);
                
                ((HibernateAssociable)childRefObj).setAssociation(
                    type, !parentIsFirstEnd, null);
                
                if (cascadeDelete) {
                    childRefObj.refDelete();
                }
            }
            emptyElements();
            delete(getHibernateRepository(item));
        } else {
            item.setAssociation(type, !parentIsFirstEnd, null);
            
            Element elem = newElement(item);
        
            children.remove(elem);
            
            if (children.isEmpty()) {
                getParent().setAssociation(type, parentIsFirstEnd, null);
                emptyElements();
                delete(getHibernateRepository(item));
            }
            
            HibernateAssociable parent = getParent();
            if (cascadeDelete && parent != null) {
                parent.refDelete();
            }            
        }
    }

    /**
     * ElementCollection wraps a Collection of 
     * {@link HibernateOneToManyLazyAssociationBase.Element} objects and
     * handles the conversion from {@link RefObject} instances to 
     * Element instances.  All operations except iteration are
     * handled without loading the RefObjects in the collection (although 
     * it is likely that any proxied RefObject passed into its methods will
     * be loaded).
     */
    private class ElementCollection
        extends AbstractCollection<HibernateAssociable>
        implements Collection<HibernateAssociable>
    {
        private final Collection<Element> collection;
        
        private ElementCollection(Collection<Element> collection)
        {
            this.collection = collection;
        }
        
        @Override
        public boolean add(HibernateAssociable o)
        {
            Element elem = newElement(o);
            
            return collection.add(elem);
        }

        @Override
        public Iterator<HibernateAssociable> iterator()
        {
            return new ElementIterator(this, collection);
        }

        @Override
        public int size()
        {
            return collection.size();
        }
        
        @Override
        public boolean contains(Object o)
        {
            Element elem = newElement((RefObject)o);
            
            return collection.contains(elem);
        }

        @Override
        public boolean remove(Object o)
        {
            Element elem = newElement((RefObject)o);
            
            return collection.remove(elem);
        }
    }
    
    /**
     * ElementIterator implements {@link Iterator} for 
     * {@link ElementCollection}.  Upon construction it materializes the 
     * collection into a List of 
     * {@link HibernateOneToManyLazyAssociationBase.Element} instances and then
     * loads the persistent {@link RefObject} instances referenced by those 
     * Element during calls to {@link #next()}. Loads are executed in batches 
     * of same-typed objects in the order in which the elements appear in the 
     * collection. If objects of different types are interleaved, 
     * ElementIterator skips ahead to find objects of the same type as that 
     * being returned by the current call.
     */
    private class ElementIterator implements Iterator<HibernateAssociable>
    {
        private final ElementCollection owner;
        private final List<Element> materializedCollection;
        private final List<HibernateAssociable> loadedObjects;
        private int size;
        private int pos;
        
        ElementIterator(
            ElementCollection owner, Collection<Element> collection)
        {
            this.owner = owner;
            this.materializedCollection = new ArrayList<Element>(collection);
            this.size = materializedCollection.size();
            this.loadedObjects = new ArrayList<HibernateAssociable>(size);
            for(int i = 0; i < size; i++) {
                loadedObjects.add(null);
            }
            
            this.pos = -1;
        }
        
        public boolean hasNext()
        {
            return (pos + 1) < size;
        }
        
        public HibernateAssociable next()
        {
            pos++;
            
            if (pos >= size) {
                throw new NoSuchElementException();
            }
            
            HibernateAssociable result = loadedObjects.get(pos);
            if (result != null) {
                return result;
            }
            
            loadBatch(materializedCollection, loadedObjects, pos, size);
            
            return loadedObjects.get(pos);
        }
        
        public void remove()
        {
            if (pos < 0 || pos >= size) {
                throw new NoSuchElementException();
            }
            
            HibernateAssociable obj = loadedObjects.remove(pos);
            materializedCollection.remove(pos);
            owner.remove(obj);
            
            pos--;
            size--;
        }
    }
}

// End HibernateOneToManyLazyAssociation.java
