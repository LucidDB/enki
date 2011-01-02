/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2010 The Eigenbase Project
// Copyright (C) 2010 SQLstream, Inc.
// Copyright (C) 2010 Dynamo BI Corporation
//
// This program is free software; you can redistribute it and/or modify it
// under the terms of the GNU General Public License as published by the Free
// Software Foundation; either version 2 of the License, or (at your option)
// any later version approved by The Eigenbase Project.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.eigenbase.enki.trans;

import java.util.*;

import javax.jmi.reflect.*;

/**
 * OwnedList intercepts add calls so that it can invoke
 * {@link TransientRefObject#markOwner}.
 *
 * TODO jvs 1-Jan-2011:  intercept remove calls for nullification as well,
 * and also intercept ListIterator.
 *
 * @author John Sichi
 * @version $Id$
 */
public class OwnedList<E extends TransientRefObject>
    implements List<E>
{
    private final List<E> wrapped;

    private final RefObject owner;

    public OwnedList(List<E> wrapped, RefObject owner)
    {
        this.wrapped = wrapped;
        this.owner = owner;
        for (E e : wrapped) {
            e.markOwner(owner);
        }
    }

    public boolean add(E e) 
    {
        e.markOwner(owner);
        return wrapped.add(e);
    }

    public void add(int index, E e) 
    {
        e.markOwner(owner);
        wrapped.add(index, e);
    }

    public boolean addAll(Collection<? extends E> c) 
    {
        for (E e : c) {
            e.markOwner(owner);
        }
        return wrapped.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends E> c) 
    {
        for (E e : c) {
            e.markOwner(owner);
        }
        return wrapped.addAll(index, c);
    }

    public void clear()
    {
        wrapped.clear();
    }

    public boolean contains(Object o) 
    {
        return wrapped.contains(o);
    }

    public boolean containsAll(Collection<?> c) 
    {
        return wrapped.containsAll(c);
    }

    public List<E> getWrapped()
    {
        return wrapped;
    }

    public boolean equals(Object o)
    {
        if (o instanceof OwnedList) {
            return wrapped.equals(((OwnedList<E>) o).getWrapped());
        }
        return wrapped.equals(o);
    }

    public E get(int index)
    {
        return wrapped.get(index);
    }

    public int hashCode()
    {
        return wrapped.hashCode();
    }

    public int indexOf(Object o)
    {
        return wrapped.indexOf(o);
    }

    public boolean isEmpty()
    {
        return wrapped.isEmpty();
    }

    public Iterator<E> iterator()
    {
        return wrapped.iterator();
    }

    public int lastIndexOf(Object o)
    {
        return wrapped.lastIndexOf(o);
    }

    public ListIterator<E> listIterator()
    {
        return wrapped.listIterator();
    }

    public ListIterator<E> listIterator(int index)
    {
        return wrapped.listIterator(index);
    }

    public E remove(int index)
    {
        return wrapped.remove(index);
    }

    public boolean remove(Object o)
    {
        return wrapped.remove(o);
    }

    public boolean removeAll(Collection<?> c) 
    {
        return wrapped.removeAll(c);
    }

    public boolean retainAll(Collection<?> c) 
    {
        return wrapped.retainAll(c);
    }

    public E set(int index, E element)
    {
        return wrapped.set(index, element);
    }

    public int size()
    {
        return wrapped.size();
    }

    public List<E> subList(int fromIndex, int toIndex)
    {
        return wrapped.subList(fromIndex, toIndex);
    }

    public Object [] toArray()
    {
        return wrapped.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return wrapped.toArray(a);
    }

    public String toString()
    {
        return wrapped.toString();
    }
}

// End OwnedList.java
