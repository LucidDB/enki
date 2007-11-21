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
package org.eigenbase.enki.prototype;

import java.util.*;

public class NotifyingArrayList<E> implements List<E>
{
    private static final long serialVersionUID = -5584349558675299663L;

    private final List<E> list;
    
    public NotifyingArrayList(List<E> list)
    {
        this.list = list;
    }

    public boolean add(E o)
    {
        System.out.println("add");
        return list.add(o);
    }

    public void add(int index, E element)
    {
        System.out.println("add@" + index);
        list.add(index, element);
    }

    public boolean addAll(Collection<? extends E> c)
    {
        System.out.println("addAll");
        return list.addAll(c);
    }

    public boolean addAll(int index, Collection<? extends E> c)
    {
        System.out.println("addAll@" + index);
        return list.addAll(index, c);
    }

    public void clear()
    {
        System.out.println("clear");
        list.clear();
    }

    public E remove(int index)
    {
        System.out.println("remove@" + index);
        return list.remove(index);
    }

    public boolean remove(Object o)
    {
        System.out.println("remove");
        return list.remove(o);
    }

    public E set(int index, E element)
    {
        System.out.println("set@" + index);
        return list.set(index, element);
    }

    public boolean contains(Object o)
    {
        return list.contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        return list.containsAll(c);
    }

    public E get(int index)
    {
        return list.get(index);
    }

    public int indexOf(Object o)
    {
        return list.indexOf(o);
    }

    public boolean isEmpty()
    {
        return list.isEmpty();
    }

    public Iterator<E> iterator()
    {
        // TODO: wrap the iterator
        return list.iterator();
    }

    public int lastIndexOf(Object o)
    {
        return list.lastIndexOf(o);
    }

    public ListIterator<E> listIterator()
    {
        // TODO: wrap the iterator
        return list.listIterator();
    }

    public ListIterator<E> listIterator(int index)
    {
        // TODO: wrap the iterator
        return list.listIterator(index);
    }

    public boolean removeAll(Collection<?> c)
    {
        System.out.println("removeAll");
        return list.removeAll(c);
    }

    public boolean retainAll(Collection<?> c)
    {
        System.out.println("retainAll");
        return list.retainAll(c);
    }

    public int size()
    {
        return list.size();
    }

    public List<E> subList(int fromIndex, int toIndex)
    {
        // TODO: wrap sublist
        return list.subList(fromIndex, toIndex);
    }

    public Object[] toArray()
    {
        return list.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        return list.toArray(a);
    }
}
