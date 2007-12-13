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
package org.eigenbase.enki.util;

import java.util.*;

/**
 * GenericCollections provides static type-safe generic collection helper 
 * methods.
 * @author Stephan Zuercher
 */
public class GenericCollections
{
    private GenericCollections()
    {
    }
    
    /**
     * Wraps the given Collection with type information.  The wrapper will
     * throw ClassCastException if you attempt to add an element that is
     * not of type E (or a subclass).  If the underlying collection does
     * not support add or remove, it will still throw.  Modifications are
     * passed through to the underlying collection.
     * 
     * @param <E> Collection element type
     * @param c collection to wrap
     * @param cls Class representing E
     * @return a wrapped collection
     * @throws ClassCastException if c contains any element that is not of
     *                            type cls.
     */
    public static <E> Collection<E> asTypedCollection(
        final Collection<?> c, final Class<E> cls)
    {
        for(Object o: c) {
            cls.cast(o);
        }
        
        return new AbstractCollection<E>() {
            private final Collection<?> collection = c;
            private final Class<E> type = cls;
            
            @Override
            @SuppressWarnings("unchecked")
            public boolean add(E elem)
            {
                type.cast(elem);
                return ((Collection<Object>)collection).add(elem);
            }
            
            @Override
            public Iterator<E> iterator()
            {
                return new Iterator<E>() {
                    private final Iterator<?> iter = collection.iterator();

                    public boolean hasNext()
                    {
                        return iter.hasNext();
                    }

                    public E next()
                    {
                        return type.cast(iter.next());
                    }

                    public void remove()
                    {
                        iter.remove();
                    }
                };
            }

            @Override
            public int size()
            {
                return collection.size();
            }
            
        };
    }
    
    /**
     * Wraps the given Collection with type information.  The wrapper will
     * throw ClassCastException if you attempt to add an element that is
     * not of type E (or a subclass).  If the underlying collection does
     * not support add or remove, it will still throw.  Modifications are
     * passed through to the underlying collection.
     * 
     * @param <E> Collection element type
     * @param c collection to wrap
     * @param cls Class representing E
     * @return a wrapped collection
     * @throws ClassCastException if c contains any element that is not of
     *                            type cls.
     */
    public static <E> List<E> asTypedList(
        final List<?> c, final Class<E> cls)
    {
        for(Object o: c) {
            cls.cast(o);
        }
        
        return new AbstractList<E>() {
            private final List<?> list = c;
            private final Class<E> type = cls;
            
            @Override
            public E get(int index)
            {
                Object o = list.get(index);
                
                return type.cast(o);
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public void add(int index, E elem)
            {
                type.cast(elem);
                ((List<Object>)list).add(index, elem);
            }
            
            @Override
            @SuppressWarnings("unchecked")
            public E set(int index, E elem)
            {
                Object o = ((List<Object>)list).set(index, elem);
                
                return type.cast(o);
            }
            
            @Override
            public E remove(int index)
            {
                Object o = list.remove(index);
                
                return type.cast(o);
            }

            @Override
            public int size()
            {
                return list.size();
            }
        };
    }
}

// End GenericCollections.java
