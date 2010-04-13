/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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
package org.eigenbase.enki.jmi.impl;

import java.util.*;

import javax.jmi.reflect.*;

/**
 * AssocQueryCollection implements a {@link Collection} of {@link RefObject}
 * that automatically updates the {@link RefAssociationBase} object responsible
 * for managing an association.  Modifications to the collection or association
 * are reflected from one to the other. 
 * 
 * @author Stephan Zuercher
 */
class AssocQueryCollection implements Collection<RefObject>
{
    protected final RefAssociationBase assoc;
    protected final RefObject fixedEnd;
    protected final boolean fixedIsFirstEnd;
    private final Collection<RefAssociationLinkImpl> links;

    public AssocQueryCollection(
        RefAssociationBase assoc,
        RefObject fixedEnd,
        boolean fixedIsFirstEnd,
        Collection<RefAssociationLinkImpl> links)
    {
        this.assoc = assoc;
        this.fixedEnd = fixedEnd;
        this.fixedIsFirstEnd = fixedIsFirstEnd;
        this.links = links;
    }

    public boolean add(RefObject o)
    {
        if (fixedIsFirstEnd) {
            return assoc.refAddLink(fixedEnd, o);
        } else {
            return assoc.refAddLink(o, fixedEnd);            
        }
    }

    public boolean addAll(Collection<? extends RefObject> c)
    {
        boolean result = false;
        for(RefObject o: c) {
            if (add(o)) {
                result = true;
            }
        }
        
        return result;
    }

    public void clear()
    {
        for(Iterator<RefObject> iter = iterator(); iter.hasNext(); ) {
            iter.next();
            iter.remove();
        }
    }

    public boolean contains(Object o)
    {
        if (o instanceof RefObject) {
            if (fixedIsFirstEnd) {
                return assoc.refLinkExists(fixedEnd, (RefObject)o);
            } else {
                return assoc.refLinkExists((RefObject)o, fixedEnd);
            }
        }
        
        return false;
    }

    public boolean containsAll(Collection<?> c)
    {
        for(Object o: c) {
            if (!(o instanceof RefObject) || !contains((RefObject)o)) {
                return false;
            }
        }
        
        return true;
    }

    public boolean isEmpty()
    {
        return links.isEmpty();
    }

    public Iterator<RefObject> iterator()
    {
        return new Iter();
    }

    public boolean remove(Object o)
    {
        if (fixedIsFirstEnd) {
            return assoc.refRemoveLink(fixedEnd, (RefObject)o);
        } else {
            return assoc.refRemoveLink((RefObject)o, fixedEnd);            
        }
    }

    public boolean removeAll(Collection<?> c)
    {
        boolean any = false;
        for(Object o: c) {
            if (remove(o)) {
                any = true;
            }
        }
        return any;
    }

    public boolean retainAll(Collection<?> c)
    {
        boolean removed = false;
        RefObject[] all = toArray(new RefObject[size()]);
        for(RefObject o: all) {
            if (!c.contains(o)) {
                if (remove(o)) {
                    removed = true;
                }
            }
        }
        return removed;
    }

    public int size()
    {
        return links.size();
    }

    public Object[] toArray()
    {
        Object[] array = links.toArray();
        if (fixedIsFirstEnd) {
            for(int i = 0; i < array.length; i++) {
                array[i] = ((RefAssociationLinkImpl)array[i]).refSecondEnd();
            }
        } else {
            for(int i = 0; i < array.length; i++) {
                array[i] = ((RefAssociationLinkImpl)array[i]).refFirstEnd();
            }
        }
        
        return array;
    }

    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] array)
    {
        int size = size();
        if (array.length < size) {
            array = 
                (T[])java.lang.reflect.Array.newInstance(
                    array.getClass().getComponentType(), size);
        }

        Object[] objArray = array;
        Iterator<RefAssociationLinkImpl> iter = links.iterator();
        for(int i = 0; i < size; i++) {
            RefAssociationLinkImpl link = iter.next();
            
            if (fixedIsFirstEnd) {
                objArray[i] = link.refSecondEnd();
            } else {
                objArray[i] = link.refFirstEnd();
            }
        }

        if (array.length > size) {
            array[size] = null;
        }

        return array;
    }
    
    private class Iter implements Iterator<RefObject>
    {
        private final List<RefAssociationLinkImpl> linksCopy;
        private final Iterator<RefAssociationLinkImpl> iter;
        private RefObject last;
        
        Iter()
        {
            this.linksCopy = new ArrayList<RefAssociationLinkImpl>(links);
            this.iter = linksCopy.iterator();
        }
        
        public boolean hasNext()
        {
            return iter.hasNext();
        }

        public RefObject next()
        {
            RefAssociationLinkImpl link = iter.next();
            
            if (fixedIsFirstEnd) {
                last = link.refSecondEnd();
            } else {
                last = link.refFirstEnd();
            }
            
            return last;
        }

        public void remove()
        {
            iter.remove();
            
            AssocQueryCollection.this.remove(last);
            last = null;
        }
    }
}

// End AssocQueryCollection.java
