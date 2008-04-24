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

/**
 * ListProxy extends {@link CollectionProxy} and implements {@link List} to 
 * assist subclasses of {@link HibernateAssociation} in the management of 
 * ordered associations with an upper bound greater than 1.
 * 
 * @author Stephan Zuercher
 */
public class ListProxy<E extends RefObject>
    extends CollectionProxy<E>
    implements List<E>
{
    private HibernateOrderedAssociation orderedAssoc;
    private List<HibernateAssociable> proxiedList;
    
    public ListProxy(
        HibernateOrderedAssociation assoc, 
        HibernateAssociable source,
        boolean firstEnd,
        String refAssocId,
        Class<E> cls)
    {
        super(assoc, source, firstEnd, refAssocId, cls);
    }
    
    public ListProxy(
        String type,
        HibernateAssociable source,
        boolean firstEnd, 
        String refAssocId,
        Class<E> cls)
    {
        super(type, source, firstEnd, refAssocId, cls);
    }
    
    E getInternal(int index)
    {
        return cls.cast(proxiedList.get(index));
    }

    protected void fireAddEvent(E e, int position)
    {
        refAssoc.fireAddEvent(firstEnd, source, e, position);
    }
    
    protected void fireRemoveEvent(E e, int position)
    {
        refAssoc.fireRemoveEvent(firstEnd, source, e, position);
    }
    
    protected void fireSetEvent(E oldE, E newE, int position)
    {
        refAssoc.fireSetEvent(firstEnd, source, oldE, newE, position);
    }
    
    public void add(int index, E element)
    {
        checkAssoc();
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }

        fireAddEvent(element, index);

        addInternal(index, element);
    }

    private void addInternal(int index, E element)
    {
        if (firstEnd) {
            orderedAssoc.add(index, source, (HibernateAssociable)element);
        } else {
            orderedAssoc.add(index, (HibernateAssociable)element, source);
        }
        
        modified();
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

    @Override
    protected void emptied()
    {
        super.emptied();
        orderedAssoc = null;
        proxiedList = null;
    }
    
    @Override
    protected void modified(
        HibernateAssociation assoc, Collection<HibernateAssociable> c)
    {
        orderedAssoc = (HibernateOrderedAssociation)assoc;
        proxiedList = (List<HibernateAssociable>)c;
    }
    
    public E get(int index)
    {
        if (orderedAssoc == null) {
            throw new IndexOutOfBoundsException();
        }
        
        return getInternal(index);
    }

    public int indexOf(Object o)
    {
        if (orderedAssoc == null) {
            return -1;
        }

        return proxiedList.indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        if (orderedAssoc == null) {
            return -1;
        }
        
        return proxiedList.lastIndexOf(o);
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
            (orderedAssoc == null && index > 0) ||
            (orderedAssoc != null && index > size()))
        {
            throw new IndexOutOfBoundsException();
        }
        
        return new IteratorProxy(index);
    }

    public E remove(int index)
    {
        if (orderedAssoc == null || index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }
        
        E element = getInternal(index);
        
        fireRemoveEvent(element, index);
        
        boolean result;
        if (firstEnd) {
            result = 
                orderedAssoc.remove(
                    index, source, (HibernateAssociable)element);
        } else {
            result = 
                orderedAssoc.remove(
                    index, (HibernateAssociable)element, source);
        }
        assert(result);
        
        if (source.getAssociation(type, firstEnd) == null) {
            // Deleted last child.
            emptied();
        }

        modified();
        return element;
    }

    public E set(int index, E element)
    {
        if (orderedAssoc == null || index < 0 || index > size()) {
            throw new IndexOutOfBoundsException();
        }
        
        E item = getInternal(index);
        
        fireSetEvent(item, element, index);
        
        if (firstEnd) {
            orderedAssoc.remove(index, source, (HibernateAssociable)item);
        } else {
            orderedAssoc.remove(index, (HibernateAssociable)item, source);
        }
        addInternal(index, element);
        return item;
    }

    public List<E> subList(int fromIndex, int toIndex)
    {
        return new SublistProxy(fromIndex, toIndex - fromIndex);
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
