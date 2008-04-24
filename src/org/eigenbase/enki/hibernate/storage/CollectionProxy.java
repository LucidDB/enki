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

import org.eigenbase.enki.hibernate.jmi.*;

/**
 * CollectionProxy implements {@link Collection} to assist subclasses of 
 * {@link HibernateAssociation} in the management of unordered associations 
 * with an upper bound greater than 1.
 * 
 * @author Stephan Zuercher
 */
public class CollectionProxy<E extends RefObject> implements Collection<E>
{
    protected final String type;
    protected final boolean firstEnd;
    protected final Class<E> cls;
    private HibernateAssociation assoc;
    protected final HibernateAssociable source;
    protected HibernateRefAssociation refAssoc;
    private int size;

    private Collection<HibernateAssociable> proxiedCollection;

    public CollectionProxy(
        HibernateAssociation assoc, 
        HibernateAssociable source,
        boolean firstEnd,
        String refAssocId,
        Class<E> cls)
    {
        this.assoc = assoc;
        this.type = assoc.getType();
        this.source = source;
        this.firstEnd = firstEnd;
        this.cls = cls;

        if (refAssocId != null) {
            this.refAssoc = 
                ((HibernateObject)assoc).getHibernateRepository().findRefAssociation(refAssocId);
        } else {
            this.refAssoc = null;
        }
        
        this.proxiedCollection = assoc.get(source);
        this.size = -1;
        
        modified(assoc, proxiedCollection);
    }
    
    public CollectionProxy(
        String type,
        HibernateAssociable source,
        boolean firstEnd, 
        String refAssocId,
        Class<E> cls)
    {
        this.assoc = null;
        this.type = type;
        this.source = source;
        this.firstEnd = firstEnd;
        this.cls = cls;

        if (refAssocId != null) {
            this.refAssoc = 
                ((HibernateRefObject)source).getHibernateRepository().findRefAssociation(
                    refAssocId);
        } else {
            this.refAssoc = null;
        }

        this.proxiedCollection = null;
        this.size = -1;
        
        modified(assoc, proxiedCollection);
    }

    protected void checkAssoc()
    {
        if (assoc == null) {
            ((HibernateObject)source).getHibernateRepository().checkTransaction(
                true);
            
            assoc = 
                (HibernateAssociation)source.getOrCreateAssociation(
                    type, firstEnd);
            
            proxiedCollection = assoc.get(source);
            
            modified(assoc, proxiedCollection);
        } else {
            ((HibernateObject)assoc).getHibernateRepository().checkTransaction(
                true);
        }
    }

    protected void fireAddEvent(E e)
    {
        refAssoc.fireAddEvent(firstEnd, source, e);
    }
    
    protected void fireRemoveEvent(E e)
    {
        refAssoc.fireRemoveEvent(firstEnd, source, e);
    }
    
    public boolean add(E e)
    {
        checkAssoc();
        
        fireAddEvent(e);
        
        if (firstEnd) {
            assoc.add(source, (HibernateAssociable)e);
        } else {
            assoc.add((HibernateAssociable)e, source);
        }

        return true;
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

    public void clear()
    {
        if (assoc != null) {
            for(HibernateAssociable a: proxiedCollection) {
                fireRemoveEvent(cls.cast(a));
            }

            assoc.clear(source);
            emptied();
        }
    }
    
    public Iterator<E> iterator()
    {
        return new IteratorProxy();
    }

    public boolean contains(Object o)
    {
        if (assoc == null) {
            return false;
        }

        return proxiedCollection.contains(o);
    }

    public boolean containsAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }

        return proxiedCollection.containsAll(c);
    }

    public boolean isEmpty()
    {
        if (assoc == null) {
            return true;
        }
        
        return proxiedCollection.isEmpty();
    }

    public boolean remove(Object o)
    {
        if (o instanceof HibernateAssociable && 
            assoc != null &&
            proxiedCollection.contains(o))
        {
            E e = cls.cast(o);
            
            fireRemoveEvent(e);
        
            return removeInternal(e);
        }
        
        return false;
    }
    
    private boolean removeInternal(E e)
    {
        boolean result;
        if (firstEnd) {
            result = assoc.remove(source, (HibernateAssociable)e);
        } else {
            result = assoc.remove((HibernateAssociable)e, source);
        }

        if (source.getAssociation(type, firstEnd) == null) {
            // Deleted last child.
            emptied();
        }

        size = -1;
        return result;
    }

    protected void emptied()
    {
        modified();
        assoc = null;
        proxiedCollection = null;
    }
    
    protected void modified()
    {
        size = -1;
    }
    
    protected void modified(
        HibernateAssociation assoc, Collection<HibernateAssociable> c)
    {
    }
    
    public boolean removeAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }
        
        boolean result = false;
        for(Object o: c) {
            if (proxiedCollection.contains(o)) {
                E e = cls.cast(o);
                fireRemoveEvent(e);
                removeInternal(e);
                result = true;
            }
        }

        size = -1;
        return result;
    }

    public boolean retainAll(Collection<?> c)
    {
        if (assoc == null) {
            return false;
        }
        
        Collection<E> toRemove = new ArrayList<E>();
        Iterator<HibernateAssociable> iter = proxiedCollection.iterator();
        while(iter.hasNext()) {
            HibernateAssociable child = iter.next();
            if (!c.contains(child)) {
                E e = cls.cast(child);
                toRemove.add(e);
            }
        }

        if (toRemove.isEmpty()) {
            return false;
        }
        
        for(E e: toRemove) {
            fireRemoveEvent(e);
            removeInternal(e);
        }

        return true;
    }
    
    public int size()
    {
        if (size == -1) {
            if (assoc != null) {
                size = proxiedCollection.size();
            } else {
                size = 0;
            }
        }
        
        return size;
    }

    public Object[] toArray()
    {
        if (assoc == null) {
            return new Object[0];
        }
        
        return proxiedCollection.toArray();
    }

    public <T> T[] toArray(T[] a)
    {
        if (assoc == null) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }
        
        return proxiedCollection.toArray(a);
    }

    /**
     * IteratorProxy implements Iterator for {@link CollectionProxy}.
     */
    private final class IteratorProxy implements Iterator<E>
    {
        private final Iterator<HibernateAssociable> iter;
        private HibernateAssociable last;
        
        IteratorProxy()
        {
            if (proxiedCollection != null) {
                this.iter = proxiedCollection.iterator();
            } else {
                Collection<HibernateAssociable> empty = 
                    Collections.emptyList();
                this.iter = empty.iterator();
            }
            this.last = null;
        }
    
        public boolean hasNext()
        {
            return iter.hasNext();
        }
    
        public E next()
        {
            last = null;
            last = iter.next();

            return cls.cast(last);
        }
    
        public void remove()
        {
            if (last == null) {
                throw new IllegalStateException();
            }

            iter.remove();
            
            if(firstEnd) {
                assoc.postRemove(source, last);
            } else {
                assoc.postRemove(last, source);
            }
        }   
    }
}

// End CollectionProxy.java
