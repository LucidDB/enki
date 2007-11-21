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

/**
 * @author Stephan Zuercher
 */
public class ListProxy<E> implements List<E>
{
    private final String type;
    private final Class<E> cls;
    private HibernateAssociation assoc;
    private final HibernateAssociable source;
    
    public ListProxy(
        HibernateAssociation assoc, 
        HibernateAssociable source,
        Class<E> cls)
    {
        this.type = assoc.getType();
        this.cls = cls;
        this.assoc = assoc;
        this.source = source;
    }
    
    public ListProxy(String type, HibernateAssociable source, Class<E> cls)
    {
        this.type = type;
        this.cls = cls;
        this.assoc = null;
        this.source = source;
    }
    
    private void checkAssoc()
    {
        if (assoc == null) {
            assoc = source.getOrCreateAssociation(type);
        }
    }

    private E getInternal(int index)
    {
        return cls.cast(assoc.get(source).get(index));
    }

    public boolean add(E e)
    {
        checkAssoc();
        assoc.add(source, (HibernateAssociable)e);
        return true;
    }

    public void add(int index, E element)
    {
        checkAssoc();
        if (index < 0 || index > assoc.get(source).size()) {
            throw new IndexOutOfBoundsException();
        }
        assoc.add(index, source, (HibernateAssociable)element);
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
            assoc.clear(source);
        }
    }
    
    public boolean contains(Object o)
    {
        if (assoc != null) {
            return false;
        }

        return assoc.get(source).contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        if (assoc != null) {
            return false;
        }

        return assoc.get(source).containsAll(c);
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

        return assoc.get(source).indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        if (assoc == null) {
            return -1;
        }
        
        return assoc.get(source).lastIndexOf(o);
    }

    public boolean isEmpty()
    {
        if (assoc == null) {
            return true;
        }
        
        return assoc.get(source).isEmpty();
    }

    public Iterator<E> iterator()
    {
        return listIterator(0);
    }

    public ListIterator<E> listIterator()
    {
        return listIterator(0);
    }

    public ListIterator<E> listIterator(int index)
    {
        if (index < 0 ||
            (assoc == null && index > 0) ||
            (assoc != null && index > assoc.get(source).size()))
        {
            throw new IndexOutOfBoundsException();
        }
        
        return new IteratorProxy(index);
    }

    public boolean remove(Object o)
    {
        if (o instanceof HibernateAssociable && assoc != null) {
            return assoc.remove(source, (HibernateAssociable)o);
        }
        
        return false;
    }

    public E remove(int index)
    {
        if (assoc == null || index < 0 || index > assoc.get(source).size()) {
            throw new IndexOutOfBoundsException();
        }
        
        E element = getInternal(index);
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
            if (assoc.get(source).contains(o)) {
                assoc.get(source).remove(o);
                result = true;
            }
        }
        
        return result;
    }

    public boolean retainAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }
        
        boolean result = false;
        List<HibernateAssociable> children = assoc.get(source);
        int i = 0;
        while(i < children.size()) {
            HibernateAssociable child = children.get(i);
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
        if (assoc == null || index < 0 || index > assoc.get(source).size()) {
            throw new IndexOutOfBoundsException();
        }
        
        E item = getInternal(index);
        assoc.remove(source, (HibernateAssociable)item);
        add(index, element);
        return item;
    }

    public int size()
    {
        if (assoc == null) {
            return 0;
        }
        
        return assoc.get(source).size();
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
        
        return assoc.get(source).toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        if (assoc == null) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        
        return assoc.get(source).toArray(a);
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
            
            return ListProxy.this.get(index + offset);
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
    
        IteratorProxy(int index)
        {
            this.index = index;
            this.offset = 0;
            this.length = -1;
        }
    
        IteratorProxy(int index, int offset, int length)
        {
            this.index = index;
            this.offset = offset;
            this.length = length;
        }
    
        public boolean hasNext()
        {
            if (offset > 0) {
                return index < length;
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

            if (offset > 0) {
                length++;
            }
        }

        public void set(E o)
        {
            ListProxy.this.set(index + offset, o);
        }
        
        public void remove()
        {
            ListProxy.this.remove(index + offset - 1);
            
            if (offset > 0) {
                length--;
            }
        }    
    }
}

// End ListProxy.java
