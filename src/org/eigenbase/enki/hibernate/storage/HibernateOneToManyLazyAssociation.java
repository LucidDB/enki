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

import org.eigenbase.enki.hibernate.*;
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
public class HibernateOneToManyLazyAssociation
    extends HibernateAssociationBase
{
    /** 
     * If true, this is a many-to-1 association.  That is, end1 is not the
     * single end.
     */
    private boolean reversed;
    
    private String parentType;
    private long parentId;
    
    private Collection<Element> children;
    
    public HibernateOneToManyLazyAssociation()
    {
        super();
     
        this.children = new HashSet<Element>();
    }
    
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
    
    public void setChildren(Collection<Element> children)
    {
        this.children = children;
    }
    
    public HibernateAssociable getParent()
    {
        String parentType = getParentType();
        if (parentType == null) {
            return null;
        }
        
        return (HibernateAssociable)load(parentType, getParentId());
    }
    
    public Collection<HibernateAssociable> getCollection()
    {
        return new ElementCollection(getChildren());
    }
    
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
                newParent.setAssociation(type, parentIsFirstEnd, this);
                // child association is brand new, just set the parent
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
        delete(getHibernateRepository(item));
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

    public void postRemove(HibernateAssociable left, HibernateAssociable right)
    {
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

            delete(getHibernateRepository(item));
        } else {
            item.setAssociation(type, !parentIsFirstEnd, null);
            
            Element elem = newElement(item);
        
            children.remove(elem);
            
            if (children.isEmpty()) {
                getParent().setAssociation(type, parentIsFirstEnd, null);
                delete(getHibernateRepository(item));
            }
            
            HibernateAssociable parent = getParent();
            if (cascadeDelete && parent != null) {
                parent.refDelete();
            }            
        }
    }

    private RefObject load(Element elem)
    {
        return load(elem.getChildType(), elem.getChildId());
    }
    
    private RefObject load(String classIdent, long mofId)
    {
        HibernateMDRepository repos = getHibernateRepository();
        
        HibernateRefClass refClass = repos.findRefClass(classIdent);

        return repos.getByMofId(mofId, refClass);
    }
    
    private List<RefObject> load(String classIdent, List<Long> mofIds)
    {
        HibernateMDRepository repos = getHibernateRepository();
        
        HibernateRefClass refClass = repos.findRefClass(classIdent);

        return repos.getByMofId(mofIds, refClass);
    }
    
    private Element newElement(RefObject obj)
    {
        HibernateRefObject hibRefObj = (HibernateRefObject)obj;
        
        Element elem = new Element();
        elem.setChildType(hibRefObj.getClassIdentifier());
        elem.setChildId(hibRefObj.getMofId());
        return elem;
    }
    
    /**
     * Element represents a member of the many-end of the association.  It
     * stores the object's type (via the {@link HibernateRefClass} unique
     * identifier) and the MOF ID of the object.  Hibernate is not aware
     * that this data refers to another persistent object, which allows us
     * to control when the referenced object is loaded.
     */
    public static class Element
    {
        protected String childType;
        protected long childId;
        
        public Element()
        {
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
        
        public boolean equals(Object other)
        {
            Element that = (Element)other;
            
            return  this.childId == that.getChildId();
        }
        
        public int hashCode()
        {
            return (int)childId;
        }
    }
    
    /**
     * ElementCollection wraps a Collection of {@link Element} objects and
     * handles the conversion from {@link RefObject} instances to 
     * {@link Element} instances.  All operations except iteration are
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
            return new ElementIterator(collection);
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
            
            if (collection.remove(elem)) {
                if (collection.isEmpty()) {
                    delete(getHibernateRepository((HibernateAssociable)o));
                }
                
                return true;
            }
            
            return false;
        }
    }
    
    /**
     * ElementIterator implements {@link Iterator} for 
     * {@link ElementCollection}.  Upon construction it materializes the 
     * collection into a List of {@link Element} instances and then loads
     * the persistent {@link RefObject} instances referenced by those 
     * Element during calls to {@link #next()}. Loads are executed in batches 
     * of same-typed objects in the order in which the elements appear in the 
     * collection. If objects of different types are interleaved, 
     * ElementIterator skips ahead to find objects of the same type as that 
     * being returned by the current call.
     */
    private class ElementIterator implements Iterator<HibernateAssociable>
    {
        private final List<Element> materializedCollection;
        private final List<HibernateAssociable> loadedObjects;
        private final int size;
        private int pos;
        
        ElementIterator(Collection<Element> collection)
        {
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
            
            List<Long> preLoadMofIds = new ArrayList<Long>();
            Map<Long, Integer> preLoadIndexes = new HashMap<Long, Integer>();
            
            Element element = materializedCollection.get(pos);
            
            final String type = element.getChildType();
            Long elementMofId = element.getChildId();
            preLoadMofIds.add(elementMofId);
            preLoadIndexes.put(elementMofId, pos);
            
            // Look for other objects in the collection with the same type
            // and load them together.
            final int batchSize = getHibernateRepository().getBatchSize();
            int numAdded = 1;
            for(int i = pos + 1; i < size && numAdded < batchSize; i++) {
                element = materializedCollection.get(i);
                if (type.equals(element.getChildType())) {
                    elementMofId = element.getChildId();
                    preLoadMofIds.add(elementMofId);
                    preLoadIndexes.put(elementMofId, i);
                    numAdded++;
                }
            }
            
            assert(numAdded == preLoadIndexes.size());
            assert(numAdded == preLoadMofIds.size());
            
            List<RefObject> objects = load(type, preLoadMofIds);
            // Assert <= because some objects may be deleted
            assert(objects.size() <= numAdded);
            
            for(RefObject obj: objects) {
                long mofId = ((HibernateRefObject)obj).getMofId();
                int index = preLoadIndexes.get(mofId);
                loadedObjects.set(index, (HibernateAssociable)obj);
            }
            
            return loadedObjects.get(pos);
        }
        
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}

// End HibernateOneToManyLazyAssociation.java
