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

import org.apache.commons.collections.*;
import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.jmi.*;

/**
 * HibernateLazyAssociationBase extends {@link HibernateAssociationBase} to
 * provide utility methods for lazy-loading associations.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateLazyAssociationBase
    extends HibernateAssociationBase
{
    /**
     * Immediately loads the given element.
     * 
     * @param elem Element to load
     * @return RefObject the Element refers to or null if not found/deleted
     */
    protected RefObject load(Element elem)
    {
        return load(elem.getChildType(), elem.getChildId());
    }

    /**
     * Immediately loads the given element.
     * 
     * @param classIdent the {@link HibernateRefClass} identifier for the
     *                   object to load
     * @param mofId the object's MOF ID
     * @return RefObject the Element refers to or null if not found/deleted
     */
    protected RefObject load(String classIdent, long mofId)
    {
        HibernateMDRepository repos = getHibernateRepository();
        
        HibernateRefClass refClass = repos.findRefClass(classIdent);
    
        return repos.getByMofId(mofId, refClass);
    }

    /**
     * Immediately loads the given elements.
     * 
     * @param classIdent the {@link HibernateRefClass} identifier for the
     *                   objects to load
     * @param mofIds list of MOF IDs for objects to load
     * @return RefObject the Element refers to or null if not found/deleted
     */
    protected Collection<RefObject> load(String classIdent, List<Long> mofIds)
    {
        HibernateMDRepository repos = getHibernateRepository();
        
        HibernateRefClass refClass = repos.findRefClass(classIdent);
    
        return repos.getByMofId(mofIds, refClass);
    }

    /**
     * Constructs an {@link Element} object from the given {@link RefObject}.
     * 
     * @param obj the RefObject to convert into an Element reference
     * @return a new Element reference to the RefObject
     */
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
        
        Collection<RefObject> objects = load(type, preLoadMofIds);
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
        assert(elements.size() == loadedObjects.size());
        
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
        
        Collection<RefObject> objects = load(type, preLoadMofIds);
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
     * Element represents a member of the many-end of an association.  It
     * stores the object's type (via the {@link HibernateRefClass} unique
     * identifier) and the MOF ID of the object.  Hibernate is not aware
     * that this data refers to another persistent object, which allows us
     * to control when the referenced object is loaded.
     */
    public static class Element
    {
        protected String childType;
        protected Long childId;
        
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
        
        public Long getChildId()
        {
            return childId;
        }
        
        public void setChildId(Long childId)
        {
            this.childId = childId;
        }
        
        public boolean equals(Object other)
        {
            Element that = (Element)other;
            
            return  this.childId.longValue() == that.getChildId().longValue();
        }
        
        public int hashCode()
        {
            return childId.intValue();
        }
    }
    

    /**
     * ElementCollection wraps a Collection of 
     * {@link HibernateLazyAssociationBase.Element} objects and
     * handles the conversion from {@link RefObject} instances to 
     * Element instances.  All operations except iteration are
     * handled without loading the RefObjects in the collection (although 
     * it is likely that any proxied RefObject passed into its methods will
     * be loaded).
     */
    protected class ElementCollection
        extends AbstractCollection<HibernateAssociable>
        implements Collection<HibernateAssociable>
    {
        private final Collection<Element> collection;
        
        protected ElementCollection(Collection<Element> collection)
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
     * {@link HibernateLazyAssociationBase.Element} instances and then
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
    

    /**
     * ElementList wraps a List of {@link HibernateLazyAssociationBase.Element}
     * objects and handles the conversion from {@link RefObject} instances to 
     * Element instances.  Most operations are handled without 
     * loading the RefObjects in the list (although it is likely that any 
     * proxied RefObject passed into its methods will be loaded).
     */
    protected class ElementList
        extends AbstractList<HibernateAssociable>
        implements List<HibernateAssociable>
    {
        protected final List<Element> elements;
        private final List<HibernateAssociable> cache;
        
        protected ElementList(List<Element> elements)
        {
            this.elements = elements;
            
            int n = elements.size();
            this.cache = new ArrayList<HibernateAssociable>(n);
            while(n-- > 0) {
                cache.add(null);
            }
        }
        
        @Override
        public HibernateAssociable get(int i)
        {
            return get(i, true);
        }
        
        @Override
        public HibernateAssociable remove(int i)
        {
            HibernateAssociable o = get(i, false);
            
            elements.remove(i);
            cache.remove(i);
            
            return o;
        }
        
        private HibernateAssociable get(int index, boolean preFetch)
        {
            HibernateAssociable result = cache.get(index);
            if (result == null) {
                if (preFetch) {
                    loadNonUniqueBatch(
                        elements, cache, index, elements.size());
                    result = cache.get(index);
                } else {
                    result = (HibernateAssociable)load(elements.get(index));
                    cache.set(index, result);
                }
            }

            return result;
        }
        
        @Override 
        public void add(int index, HibernateAssociable e)
        {
            Element elem = newElement(e);
            
            elements.add(index, elem);
            cache.add(index, e);
        }
        
        @Override
        public HibernateAssociable set(int i, HibernateAssociable newValue)
        {
            Element newElement = newElement(newValue);
            Element oldElement = elements.get(i);
            HibernateAssociable oldValue = get(i, false);
            
            if (!newElement.equals(oldElement)) {
                // Only modify the elements if it's a new element.
                elements.set(i, newElement);
                cache.set(i, newValue);
            }

            return oldValue;
        }
        
        @Override
        public int size()
        {
            return elements.size();
        }
        
        @Override
        public boolean contains(Object o)
        {
            Element elem = newElement((RefObject)o);
            
            return elements.contains(elem);
        }

        @Override
        public int indexOf(Object o)
        {
            Element elem = newElement((RefObject)o);
            
            return elements.indexOf(elem);
        }

        @Override
        public int lastIndexOf(Object o)
        {
            Element elem = newElement((RefObject)o);
            
            return elements.lastIndexOf(elem);
        }

        @Override
        public boolean remove(Object o)
        {
            Element elem = newElement((RefObject)o);
            
            int index = elements.indexOf(elem);
            if (index < 0) {
                return false;
            }
            
            remove(index);
            return true;
        }
    }
}

// End HibernateLazyAssociationBase.java
