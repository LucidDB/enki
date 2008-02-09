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
 * AttributeCollectionWrapper wraps {@Collection} instances used to implement 
 * multi-valued non-entity attributes.
 *
 * @author Stephan Zuercher
 */
public class AttributeCollectionWrapper<E> implements Collection<E>
{
    protected final RefObject source;
    protected final String attributeName;
    private final Collection<E> coll;
    
    public AttributeCollectionWrapper(
        RefObject source, 
        String attributeName, 
        Collection<E> coll)
    {
        this.source = source;
        this.attributeName = attributeName;
        this.coll = coll;
    }
    
    public boolean add(E o)
    {
        fireAddEvent(o);
        return coll.add(o);
    }

    public boolean addAll(Collection<? extends E> c)
    {
        for(E e: c) {
            fireAddEvent(e);
        }
        return coll.addAll(c);
    }

    public void clear()
    {
        for(E e: coll) {
            fireRemoveEvent(e);
        }
        coll.clear();
    }

    public boolean contains(Object o)
    {
        return coll.contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        return coll.containsAll(c);
    }

    public boolean isEmpty()
    {
        return coll.isEmpty();
    }

    public Iterator<E> iterator()
    {
        return new Itr(coll.iterator());
    }

    public boolean remove(Object o)
    {
        if (coll.contains(o)) {
            fireRemoveEvent(o);
        }
        
        return coll.remove(o);
    }

    public boolean removeAll(Collection<?> c)
    {
        for(Object o: c) {
            if (coll.contains(o)) {
                fireRemoveEvent(o);
            }
        }
        
        return coll.removeAll(c);
    }

    public boolean retainAll(Collection<?> c)
    {
        for(Object o: c) {
            if (!coll.contains(o)) {
                fireRemoveEvent(o);
            }
        }
        
        return coll.retainAll(c);
    }

    public int size()
    {
        return coll.size();
    }

    public Object[] toArray()
    {
        return coll.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return coll.toArray(a);
    }
    
    protected void fireAddEvent(Object o)
    {
        HibernateMDRepository.enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_ADD,
                attributeName,
                null,
                o,
                AttributeEvent.POSITION_NONE));                
    }

    protected void fireRemoveEvent(Object o)
    {
        HibernateMDRepository.enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_REMOVE,
                attributeName,
                o,
                null,
                AttributeEvent.POSITION_NONE));
    }
    
    private class Itr implements Iterator<E>
    {
        private final Iterator<E> iter;
        private boolean lastValid;
        private E last;
        
        private Itr(Iterator<E> iter)
        {
            this.iter = iter;
            this.lastValid = false;
        }

        public boolean hasNext()
        {
            return iter.hasNext();
        }

        public E next()
        {
            lastValid = false;
            last = iter.next();
            lastValid = true;
            return last;
        }

        public void remove()
        {
            if (lastValid) {
                fireRemoveEvent(last);
                lastValid = false;
            }
            
            iter.remove();
        }
    }
}

// End AttributeCollectionWrapper.java
