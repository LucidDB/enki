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
import org.netbeans.api.mdr.events.*;

/**
 * AttributeListWrapper wraps {@List} instances used to implement multi-valued
 * non-entity attributes.
 * 
 * @author Stephan Zuercher
 */
public class AttributeListWrapper<E> 
    extends AttributeCollectionWrapper<E>
    implements List<E>
{
    private final List<E> list;
    private final int offset;
    
    public AttributeListWrapper(
        RefObject source,
        String attributeName,
        List<E> list,
        Class<E> cls)
    {
        this(source, attributeName, list, 0);
    }
    
    private AttributeListWrapper(
        RefObject source,
        String attributeName,
        List<E> list,
        int offset)
    {
        super(source, attributeName, list);
        this.list = list;
        this.offset = offset;
    }

    public void add(int index, E element)
    {
        if (index >= 0 && index <= size()) {
            fireAddEvent(element, index + offset);
        }
        
        list.add(index, element);
    }

    public boolean addAll(int index, Collection<? extends E> c)
    {
        if (index >= 0 && index <= size()) {
            int position = index;
            for(E e: c) {
                fireAddEvent(e, position++ + offset);
            }
        }
        
        return list.addAll(index, c);
    }

    public E get(int index)
    {
        return list.get(index);
    }

    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }

    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    public ListIterator<E> listIterator()
    {
        return new ListItr(list.listIterator());
    }

    public ListIterator<E> listIterator(int index)
    {
        return new ListItr(list.listIterator(index));
    }

    public E remove(int index)
    {
        E e = list.get(index);
        fireRemoveEvent(e, index + offset);
        return list.remove(index);
    }

    public E set(int index, E element)
    {
        E oldElement = list.get(index);
        fireSetEvent(oldElement, element, index + offset);
        return list.set(index, element);
    }

    public List<E> subList(int fromIndex, int toIndex)
    {
        return new AttributeListWrapper<E>(
            source,
            attributeName,
            list.subList(fromIndex, toIndex),
            fromIndex);
    }
 
    protected void fireAddEvent(Object o, int position)
    {
        HibernateMDRepository.enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_ADD,
                attributeName,
                null,
                o,
                position));                
    }

    protected void fireRemoveEvent(Object o, int position)
    {
        HibernateMDRepository.enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_REMOVE,
                attributeName,
                o,
                null,
                position));
    }

    protected void fireSetEvent(Object oldObj, Object newObj, int position)
    {
        HibernateMDRepository.enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_SET,
                attributeName,
                oldObj,
                newObj,
                position));
    }
    
    private class ListItr implements ListIterator<E>
    {
        private final ListIterator<E> iter;
        private boolean lastValid;
        private E last;
        
        private ListItr(ListIterator<E> iter)
        {
            this.iter = iter;
            this.lastValid = false;
        }

        public void add(E o)
        {
            fireAddEvent(o);
            iter.add(o);
            lastValid = false;
        }

        public boolean hasNext()
        {
            return iter.hasNext();
        }

        public boolean hasPrevious()
        {
            return iter.hasPrevious();
        }

        public E next()
        {
            lastValid = false;
            last = iter.next();
            lastValid = true;
            return last;
        }

        public int nextIndex()
        {
            return iter.nextIndex();
        }

        public E previous()
        {
            lastValid = false;
            last = iter.previous();
            lastValid = true;
            return last;
        }

        public int previousIndex()
        {
            return iter.previousIndex();
        }

        public void remove()
        {
            if (lastValid) {
                fireRemoveEvent(last);
                lastValid = false;
            }
            
            iter.remove();
        }

        public void set(E o)
        {
            if (lastValid) {
                fireSetEvent(last, o, AttributeEvent.POSITION_NONE);
                lastValid = false;
            }
            
            iter.set(o);
        }
    }
}

// End AttributeListWrapper.java
