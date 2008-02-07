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

import org.eigenbase.enki.hibernate.jmi.*;

/**
 * ListProxy implements {@link List} to assist subclasses of 
 * {@link HibernateAssociation} in the management of associations with
 * an upper bound greater than 1.
 * 
 * @author Stephan Zuercher
 */
public class ListProxy<E extends RefObject> implements List<E>
{
    private final String type;
    private final boolean firstEnd;
    private final Class<E> cls;
    private HibernateAssociation assoc;
    private final HibernateAssociable source;
    private List<HibernateAssociable> proxiedList;
    private int size;
    private HibernateRefAssociation refAssoc;
    
    public ListProxy(
        HibernateAssociation assoc, 
        HibernateAssociable source,
        boolean firstEnd,
        String refAssocId,
        Class<E> cls)
    {
        this.assoc = assoc;
        this.type = assoc.getType();
        this.source = source;
        this.firstEnd = firstEnd;
        this.cls = cls;

        if (refAssocId != null) {
            this.refAssoc = 
                HibernateRefAssociationRegistry.instance().findRefAssociation(
                    refAssocId);
        } else {
            this.refAssoc = null;
        }

        this.proxiedList = assoc.get(source);
        this.size = -1;
    }
    
    public ListProxy(
        String type,
        HibernateAssociable source,
        boolean firstEnd, 
        String refAssocId,
        Class<E> cls)
    {
        this.assoc = null;
        this.type = type;
        this.source = source;
        this.firstEnd = firstEnd;
        this.cls = cls;

        if (refAssocId != null) {
            this.refAssoc = 
                HibernateRefAssociationRegistry.instance().findRefAssociation(
                    refAssocId);
        } else {
            this.refAssoc = null;
        }

        this.proxiedList = null;
        this.size = -1;
    }
    
    private void checkAssoc()
    {
        if (assoc == null) {
            assoc = source.getOrCreateAssociation(type, firstEnd);
            
            proxiedList = assoc.get(source);
        }
    }

    E getInternal(int index)
    {
        return cls.cast(proxiedList.get(index));
    }

    private void fireAddEvent(E e, int position)
    {
        if (refAssoc != null) {
            refAssoc.fireAddEvent(firstEnd, source, e, position);
        }
    }
    
    private void fireRemoveEvent(E e, int position)
    {
        if (refAssoc != null) {
            refAssoc.fireRemoveEvent(firstEnd, source, e, position);
        }
    }
    
    private void fireSetEvent(E oldE, E newE, int position)
    {
        if (refAssoc != null) {
            refAssoc.fireSetEvent(firstEnd, source, oldE, newE, position);
        }
    }
    
    public boolean add(E e)
    {
        checkAssoc();

        fireAddEvent(e, size);
        
        if (firstEnd) {
            assoc.add(source, (HibernateAssociable)e);
        } else {
            assoc.add((HibernateAssociable)e, source);
        }

        size = -1;
        return true;
    }

    public void add(int index, E element)
    {
        checkAssoc();
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }

        // TODO: EVENT: set events for re-indexed values?
        fireAddEvent(element, index);
        
        if (firstEnd) {
            assoc.add(index, source, (HibernateAssociable)element);
        } else {
            assoc.add(index, (HibernateAssociable)element, source);
        }
        
        size = -1;
    }

    public boolean addAll(Collection<? extends E> c)
    {
        boolean result = false;
        for(E e: c) {
            if (add(e)) {
                result = true;
            }
        }
        
        return result;
    }

    public boolean addAll(int index, Collection<? extends E> c)
    {
        boolean result = false;
        for(E e: c) {
            add(index++, e);
            result = true;
        }
        
        return result;
    }

    public void clear()
    {
        if (assoc != null) {
            for(int i = 0; i < size; i++) {
                fireRemoveEvent(getInternal(i), i);
            }

            assoc.clear(source);
            assoc = null;
            proxiedList = null;
            size = -1;
        }
    }
    
    public boolean contains(Object o)
    {
        if (assoc == null) {
            return false;
        }

        return proxiedList.contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }

        return proxiedList.containsAll(c);
    }

    public E get(int index)
    {
        if (assoc == null) {
            throw new IndexOutOfBoundsException();
        }
        
        return getInternal(index);
    }

    public int indexOf(Object o)
    {
        if (assoc == null) {
            return -1;
        }

        return proxiedList.indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        if (assoc == null) {
            return -1;
        }
        
        return proxiedList.lastIndexOf(o);
    }

    public boolean isEmpty()
    {
        if (assoc == null) {
            return true;
        }
        
        return proxiedList.isEmpty();
    }

    public Iterator<E> iterator()
    {
        return new IteratorProxy(0);
    }

    public ListIterator<E> listIterator()
    {
        return new IteratorProxy(0);
    }

    public ListIterator<E> listIterator(int index)
    {
        if (index < 0 ||
            (assoc == null && index > 0) ||
            (assoc != null && index > size()))
        {
            throw new IndexOutOfBoundsException();
        }
        
        return new IteratorProxy(index);
    }

    public boolean remove(Object o)
    {
        if (o instanceof HibernateAssociable && assoc != null) {
            int index = proxiedList.indexOf(o);
            fireRemoveEvent(cls.cast(o), index);            
            
            boolean result;
            if (firstEnd) {
                result = assoc.remove(source, (HibernateAssociable)o);
            } else {
                result = assoc.remove((HibernateAssociable)o, source);
            }

            if (source.getAssociation(type, firstEnd) == null) {
                // Deleted last child.
                assoc = null;
                proxiedList = null;
            }

            size = -1;
            return result;
        }
        
        return false;
    }

    public E remove(int index)
    {
        if (assoc == null || index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }
        
        E element = getInternal(index);
        
        fireRemoveEvent(element, index);
        
        remove(element);
        return element;
    }

    public boolean removeAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }
        
        boolean result = false;
        for(Object o: c) {
            int index = proxiedList.indexOf(o);
            if (index >= 0) {
                fireRemoveEvent(cls.cast(o), index);
                proxiedList.remove(index);
                result = true;
            }
        }

        size = -1;
        return result;
    }

    public boolean retainAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }
        
        boolean result = false;

        int i = 0;
        while(i < proxiedList.size()) {
            HibernateAssociable child = proxiedList.get(i);
            if (!c.contains(child)) {
                remove(i);
                result = true;
            } else {
                i++;
            }
        }

        return result;
    }

    public E set(int index, E element)
    {
        if (assoc == null || index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }
        
        E item = getInternal(index);
        
        fireSetEvent(item, element, index);
        
        if (firstEnd) {
            assoc.remove(source, (HibernateAssociable)item);
        } else {
            assoc.remove((HibernateAssociable)item, source);
        }
        add(index, element);
        return item;
    }

    public int size()
    {
        if (size == -1) {
            if (assoc != null) {
                size = proxiedList.size();
            } else {
                size = 0;
            }
        }
        
        return size;
    }

    public List<E> subList(int fromIndex, int toIndex)
    {
        return new SublistProxy(fromIndex, toIndex - fromIndex);
    }

    public Object[] toArray()
    {
        if (assoc == null) {
            return new Object[0];
        }
        
        return proxiedList.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        if (assoc == null) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        
        return proxiedList.toArray(a);
    }
    
    private final class SublistProxy
        extends AbstractList<E>
    {
        private final int offset;
        private int length;

        private SublistProxy(int offset, int length)
        {
            this.offset = offset;
            this.length = length;
        }

        @Override
        public E get(int index)
        {
            if (index >= length) {
                throw new IndexOutOfBoundsException();
            }
            
            return ListProxy.this.getInternal(index + offset);
        }

        @Override
        public int size()
        {
            return length;
        }

        @Override
        public Iterator<E> iterator()
        {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator()
        {
            return listIterator(0);
        }

        @Override
        public ListIterator<E> listIterator(int startIndex)
        {
            return new IteratorProxy(startIndex, offset, length);
        }
    }

    /**
     * IteratorProxy implements ListIterator for {@link ListProxy}.
     * The offset and length fields are used for the List returned by a call
     * to {@link ListProxy#subList(int, int)}.
     */
    private final class IteratorProxy implements ListIterator<E>
    {
        private final int offset;
        private int length;
        
        private int index;
    
        private boolean modified;
        
        IteratorProxy(int index)
        {
            this.index = index;
            this.offset = 0;
            this.length = -1;
            this.modified = false;
        }
    
        IteratorProxy(int index, int offset, int length)
        {
            this.index = index;
            this.offset = offset;
            this.length = length;
            this.modified = false;
        }
    
        public boolean hasNext()
        {
            int len = length;
            if (len >= 0) {
                return index < len;
            } else {
                return index < ListProxy.this.size();
            }
        }
    
        public boolean hasPrevious()
        {
            return index > 0;
        }
    
        public E next()
        {
            modified = false;

            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            return ListProxy.this.getInternal(index++ + offset);
        }
    
        public int nextIndex()
        {
            return index;
        }
    
        public E previous()
        {
            modified = false;

            if (!hasPrevious()) {
                throw new NoSuchElementException();
            }
            
            return ListProxy.this.getInternal(--index + offset);                    
        }
    
        public int previousIndex()
        {
            return index - 1;
        }
    
        public void add(E o)
        {
            ListProxy.this.add(index + offset, o);
            
            index++;
            if (length >= 0) {
                length++;
            }
            modified = true;
        }

        public void set(E o)
        {
            if (modified) {
                throw new IllegalStateException(
                    "set() after add() or remove() w/o intervening next()");
            }
            
            if (index == 0) {
                throw new IllegalStateException("set() before next()");
            }
            
            ListProxy.this.set(index + offset - 1, o);
        }
        
        public void remove()
        {
            if (modified) {
                throw new IllegalStateException(
                    "remove() after add() or remove() w/o intervening next()");
            }
                    
            if (index == 0) {
                throw new IllegalStateException("remove() before next()");
            }

            index--;
            ListProxy.this.remove(index + offset);
            
            if (length >= 0) {
                length--;
            }

            modified = true;
        }   
    }
}

// End ListProxy.java
