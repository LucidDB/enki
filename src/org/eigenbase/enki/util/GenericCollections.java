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
 * 
 * @author Stephan Zuercher
 */
public class GenericCollections
{
    private GenericCollections()
    {
    }
    
    /**
     * Wraps the given Collection with type information.  The wrapper is
     * identical to the one provided by 
     * {@link Collections#checkedCollection(Collection, Class)} or
     * {@link Collections#checkedSet(Set, Class)}, depending on whether the
     * given collection implements {@link Set}.  This method verifies that 
     * all members of the given collection are of the given type before 
     * returning.
     * 
     * @param <E> Collection element type
     * @param c collection to wrap
     * @param cls Class representing E
     * @return a wrapped collection
     * @throws ClassCastException if c contains any element that is not of
     *                            type cls.
     */
    @SuppressWarnings("unchecked")
    public static <E> Collection<E> asTypedCollection(
        final Collection<?> c, final Class<E> cls)
    {
        for(Object o: c) {
            cls.cast(o);
        }
        
        if (c instanceof Set) {
            // Use a class that implements Set<E> or else the equals method
            // will not work with JDK Set implementations.
            return Collections.checkedSet((Set<E>)c, cls);
        }
        
        return Collections.checkedCollection((Collection<E>)c, cls);
    }
    
    /**
     * Wraps the given List with type information.  The wrapper is
     * identical to the one provided by 
     * {@link Collections#checkedList(List, Class)}. This method 
     * verifies that all members of the given collection are of the given type 
     * before returning.
     * 
     * @param <E> Collection element type
     * @param list list to wrap
     * @param cls Class representing E
     * @return a wrapped list
     * @throws ClassCastException if c contains any element that is not of
     *                            type cls.
     */
    @SuppressWarnings("unchecked")
    public static <E> List<E> asTypedList(
        final List<?> list, final Class<E> cls)
    {
        for(Object o: list) {
            cls.cast(o);
        }

        return Collections.checkedList((List<E>)list, cls);
    }
}

// End GenericCollections.java
