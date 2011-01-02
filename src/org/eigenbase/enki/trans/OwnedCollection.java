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
 * OwnedCollection intercepts add calls so that it can invoke
 * {@link TransientRefObject#markOwner}.
 *
 * TODO jvs 1-Jan-2011:  intercept remove calls for nullification as well,
 * and also intercept Iterator.
 *
 * @author John Sichi
 * @version $Id$
 */
public class OwnedCollection<E extends TransientRefObject>
    implements Collection<E>
{
    private final Collection<E> wrapped;

    private final RefObject owner;

    public OwnedCollection(Collection<E> wrapped, RefObject owner)
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

    public boolean addAll(Collection<? extends E> c) 
    {
        for (E e : c) {
            e.markOwner(owner);
        }
        return wrapped.addAll(c);
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

    public Collection<E> getWrapped()
    {
        return wrapped;
    }

    public boolean equals(Object o)
    {
        if (o instanceof OwnedCollection) {
            return wrapped.equals(((OwnedCollection<E>) o).getWrapped());
        }
        return wrapped.equals(o);
    }

    public int hashCode()
    {
        return wrapped.hashCode();
    }

    public boolean isEmpty()
    {
        return wrapped.isEmpty();
    }

    public Iterator<E> iterator()
    {
        return wrapped.iterator();
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

    public int size()
    {
        return wrapped.size();
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

// End OwnedCollection.java
