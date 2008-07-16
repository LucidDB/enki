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

import org.apache.commons.collections.*;
import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.jmi.*;

/**
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyLazyAssociationBase
    extends HibernateAssociationBase
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
        String parentType = getParentType();
        if (parentType == null) {
            return null;
        }
        
        return (HibernateAssociable)load(parentType, getParentId());
    }

    protected abstract boolean getUnique();
    
    protected abstract Collection<Element> getElements();
    protected abstract void emptyElements();
    protected abstract Collection<HibernateAssociable> getCollection();
    
    public abstract void addInitialChild(HibernateAssociable child);

    public abstract void setInitialParent(HibernateAssociable parent);

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
                while(childAssoc.getElements().remove(elem));
                if (childAssoc.getElements().isEmpty()) {
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
                        childElement.getChildId());
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
    
    protected RefObject load(Element elem)
    {
        return load(elem.getChildType(), elem.getChildId());
    }

    protected RefObject load(String classIdent, long mofId)
    {
        HibernateMDRepository repos = getHibernateRepository();
        
        HibernateRefClass refClass = repos.findRefClass(classIdent);
    
        return repos.getByMofId(mofId, refClass);
    }

    protected List<RefObject> load(String classIdent, List<Long> mofIds)
    {
        HibernateMDRepository repos = getHibernateRepository();
        
        HibernateRefClass refClass = repos.findRefClass(classIdent);
    
        return repos.getByMofId(mofIds, refClass);
    }

    protected Element newElement(RefObject obj)
    {
        HibernateRefObject hibRefObj = (HibernateRefObject)obj;
        
        Element elem = new Element();
        elem.setChildType(hibRefObj.getClassIdentifier());
        elem.setChildId(hibRefObj.getMofId());
        return elem;
    }
    
    /**
     * Scans the list of elements and pre-loads additional objects of the
     * same type.  Pre-load batch size is controlled by
     * {@link HibernateMDRepository#getBatchSize()}.
     * 
     * @param elements list of Element objects
     * @param loadedObjects list of previously loaded objects corresponding to
     *                      elements index-by-index
     * @param pos index of element to load
     * @param size size of elements and loadedObjects
     */
    protected void loadBatch(
        List<Element> elements,
        List<HibernateAssociable> loadedObjects, 
        final int pos,
        final int size)
    {
        List<Long> preLoadMofIds = new ArrayList<Long>();
        Map<Long, Integer> preLoadIndexes = new HashMap<Long, Integer>();
        
        Element element = elements.get(pos);
        
        final String type = element.getChildType();
        Long elementMofId = element.getChildId();
        preLoadMofIds.add(elementMofId);
        preLoadIndexes.put(elementMofId, pos);
        
        // Look for other objects in the collection with the same type
        // and load them together.
        final int batchSize = getHibernateRepository().getBatchSize();
        int numAdded = 1;
        for(
            int i = (pos + 1) % size; 
            i != pos && numAdded < batchSize; 
            i = (i + 1) % size)
        {
            element = elements.get(i);
            if (loadedObjects.get(i) == null &&
                type.equals(element.getChildType()))
            {
                elementMofId = element.getChildId();
                preLoadMofIds.add(elementMofId);
                preLoadIndexes.put(elementMofId, i);
                numAdded++;
            }
        }
        
        assert(numAdded == preLoadIndexes.size());
        assert(numAdded == preLoadMofIds.size());
        
        List<RefObject> objects = load(type, preLoadMofIds);
        assert(objects.size() == numAdded);
        
        for(RefObject obj: objects) {
            long mofId = ((HibernateRefObject)obj).getMofId();
            int index = preLoadIndexes.get(mofId);
            loadedObjects.set(index, (HibernateAssociable)obj);
        }
    }
    
    /**
     * Scans the list of elements and pre-loads additional objects of the
     * same type.  See {@link #loadBatch(List, List, int, int)}.  The primary
     * distinction is that this method handles ordered semantics, where an
     * object can appear in the list multiple times.
     * 
     * @param elements list of Element objects
     * @param loadedObjects list of previously loaded objects corresponding to
     *                      elements index-by-index
     * @param pos index of element to load
     * @param size size of elements and loadedObjects
     */
    protected void loadNonUniqueBatch(
        List<Element> elements,
        List<HibernateAssociable> loadedObjects,
        final int pos,
        final int size)
    {
        // TODO: SWZ: 2008-07-14: These is nearly identical to loadBatch,
        // except for the multi-map usage.  Perhaps simply combine them.
        List<Long> preLoadMofIds = new ArrayList<Long>();
        MultiHashMap preLoadIndexes = new MultiHashMap();
        
        Element element = elements.get(pos);
        
        final String type = element.getChildType();
        Long elementMofId = element.getChildId();
        preLoadMofIds.add(elementMofId);
        preLoadIndexes.put(elementMofId, pos);
        
        // Look for other objects in the collection with the same type
        // and load them together.
        final int batchSize = getHibernateRepository().getBatchSize();
        int numAdded = 1;
        for(
            int i = (pos + 1) % size; 
            i != pos && numAdded < batchSize; 
            i = (i + 1) % size)
        {
            element = elements.get(i);
            if (loadedObjects.get(i) == null && 
                type.equals(element.getChildType()))
            {
                elementMofId = element.getChildId();
                if (!preLoadIndexes.containsKey(elementMofId)) {
                    preLoadMofIds.add(elementMofId);
                    numAdded++;                    
                }
                preLoadIndexes.put(elementMofId, i);
            }
        }
        
        assert(numAdded == preLoadIndexes.size());
        assert(numAdded == preLoadMofIds.size());
        
        List<RefObject> objects = load(type, preLoadMofIds);
        assert(objects.size() == numAdded);
        
        for(RefObject obj: objects) {
            long mofId = ((HibernateRefObject)obj).getMofId();
            Collection<?> indexes = (Collection<?>)preLoadIndexes.get(mofId);
            
            for(Object indexObj: indexes) {
                Integer index = (Integer)indexObj;
                loadedObjects.set(index, (HibernateAssociable)obj);
            }
        }
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

}

// End HibernateOneToManyLazyAssociationBase.java
