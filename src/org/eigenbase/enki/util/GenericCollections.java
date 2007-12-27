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
     * Wraps the given Collection with type information.  The wrapper will
     * throw ClassCastException if you attempt to add an element that is
     * not of type E (or a subclass).  If the underlying collection does
     * not support add or remove, it will still throw.  Modifications are
     * passed through to the underlying collection.  Modifications to the
     * underlying collection are reflected in the returned collection, and
     * all restrictions on concurrent modification remain in effect.
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
        
        if (c instanceof Set) {
            // Use a class that implements Set<E> or else the equals method
            // will not work with JDK Set implementations.
            return new TypedSet<E>(cls, (Set<?>)c);
        }
        
        return new TypedCollection<E>(cls, c);
    }
    
    /**
     * Wraps the given Collection with type information.  The wrapper will
     * throw ClassCastException if you attempt to add an element that is
     * not of type E (or a subclass).  If the underlying collection does
     * not support add or remove, it will still throw.  Modifications are
     * passed through to the underlying collection.  Modifications to the
     * underlying list are reflected in the returned list, and all 
     * restrictions on concurrent modification remain in effect.
     * 
     * @param <E> Collection element type
     * @param list list to wrap
     * @param cls Class representing E
     * @return a wrapped list
     * @throws ClassCastException if c contains any element that is not of
     *                            type cls.
     */
    public static <E> List<E> asTypedList(
        final List<?> list, final Class<E> cls)
    {
        for(Object o: list) {
            cls.cast(o);
        }
        
        if (list instanceof RandomAccess) {
            // Use a class that implements RandomAccess to allow methods
            // in java.util.Collections to use optimal algorithms for the
            // delegate list.
            return new RandomAccessTypedList<E>(cls, list);
        }
        
        return new TypedList<E>(cls, list);
    }
    
    /**
     * TypedList is a generically typed list that delegates to an underlying
     * wildcard list.  All members of the delegate list must be assignable
     * to the generic type specified at the time of construction.
     * 
     * <p>Changes to this list are reflected in the delegate and vice versa.
     * Restrictions on concurrent modification of the delegate apply to 
     * instances of this class as well.   In particular, if the delegate must 
     * be externally synchronized before modification, you must arrange to do
     * this outside the scope of TypedList.
     * 
     * <p>Efficiency of operations is equivalent to the deletegate's
     * efficiency plus the constant overhead of calls to the delegate and
     * casting operations. 
     * 
     * <p>Note that some algorithms in {@link Collections} may operate with
     * reduced efficiency since TypedList does not implement 
     * {@link RandomAccess}.  See {@link RandomAccessTypedList}.
     */
    private static final class TypedList<E> extends AbstractTypedList<E>
    {
        /**
         * Constructs a new TypedList with the given type which delegates to
         * the given list.  All existing members of <code>l</code> must be
         * assignable to a variable of type <code>cls</code>.
         * 
         * @param cls list element type
         * @param l delegate list
         */
        private TypedList(Class<E> cls, List<?> l)
        {
            super(cls, l);
        }
    }
    
    /**
     * RandomAccessTypedList is identical to {@link TypedList} but implements
     * the {@link RandomAccess} interface as a hint to list-based algorithms.
     */
    private static final class RandomAccessTypedList<E> 
        extends AbstractTypedList<E>
        implements RandomAccess
    {
        /**
         * Constructs a new RandomAccessTypedList with the given type which 
         * delegates to the given list.  The parameter <code>l</code> must
         * implement the {@link RandomAccess} interface.  All existing members 
         * of <code>l</code> must be assignable to a variable of type 
         * <code>cls</code>.
         * 
         * @param cls list element type
         * @param l delegate list
         * @throws ClassCastException if <code>l</code> does not implement
         *                            {@link RandomAccess}
         */
        private RandomAccessTypedList(Class<E> cls, List<?> l)
        {
            super(cls, l);
            
            RandomAccess.class.cast(l);
        }

        @Override
        protected List<E> newSubList(int fromIndex, int toIndex)
        {
            return new RandomAccessSubList(fromIndex, toIndex);
        }
        
        /**
         * RandomAccessSubList tags {@link AbstractTypedList.SubList} with
         * the {@link RandomAccess} interface.
         */
        private class RandomAccessSubList
            extends AbstractTypedList<E>.SubList
            implements RandomAccess
        {
            private RandomAccessSubList(int fromIndex, int toIndex)
            {
                super(fromIndex, toIndex);
            }
        }
    }
    
    private static abstract class AbstractTypedList<E>
        extends AbstractTypedCollection<E>
        implements List<E>
    {
        protected final List<?> list;
    
        private AbstractTypedList(Class<E> cls, List<?> c)
        {
            super(cls, c);
            
            this.list = c;            
        }
    
        public E get(int index)
        {
            return type.cast(list.get(index));
        }
    
        @SuppressWarnings("unchecked")
        public void add(int index, E elem)
        {
            ((List<Object>)list).add(index, elem);
        }
    
        @SuppressWarnings("unchecked")
        public E set(int index, E elem)
        {
            Object o = ((List<Object>)list).set(index, elem);
            
            return type.cast(o);
        }
    
        public E remove(int index)
        {
            return type.cast(list.remove(index));
        }
    
        @SuppressWarnings("unchecked")
        public boolean addAll(int index, Collection<? extends E> c)
        {
            return ((List<Object>)list).addAll(index, c);
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
            return new TypedListIterator();
        }

        public ListIterator<E> listIterator(int index)
        {
            return new TypedListIterator(index);
        }

        public List<E> subList(int fromIndex, int toIndex)
        {
            return newSubList(fromIndex, toIndex);
        }
        
        protected List<E> newSubList(int fromIndex, int toIndex)
        {
            return new SubList(fromIndex, toIndex);
        }
        
        private class TypedListIterator implements ListIterator<E>
        {
            private final ListIterator<?> iter;
            
            private TypedListIterator()
            {
                this.iter = list.listIterator();
            }

            private TypedListIterator(int index)
            {
                this.iter = list.listIterator(index);
            }

            @SuppressWarnings("unchecked")
            public void add(E o)
            {
                ((ListIterator<Object>)iter).add(o);
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
                return type.cast(iter.next());
            }

            public int nextIndex()
            {
                return iter.nextIndex();
            }

            public E previous()
            {
                return type.cast(iter.previous());
            }

            public int previousIndex()
            {
                return iter.previousIndex();
            }

            public void remove()
            {
                iter.remove();
            }

            @SuppressWarnings("unchecked")
            public void set(E o)
            {
                ((ListIterator<Object>)iter).set(o);
            }
        }
        
        protected class SubList extends AbstractList<E>
        {
            private final int offset;
            private int size;

            private SubList(int fromIndex, int toIndex)
            {
                this.offset = fromIndex;
                this.size = toIndex - fromIndex;
            }

            private void check(int index)
            {
                if (index<0 || index >= size) {
                    throw new IndexOutOfBoundsException(
                        "Index: " + index + "; Size: " + size);
                }
            }
            
            @Override
            public E get(int index)
            {
                check(index);
                return AbstractTypedList.this.get(offset + index);
            }

            @Override
            public int size()
            {
                return size;
            }

            @Override
            public void add(int index, E o)
            {
                check(index);
                AbstractTypedList.this.add(offset + index, o);
                size++;
            }

            @Override
            public E remove(int index)
            {
                check(index);
                E removed = AbstractTypedList.this.remove(offset + index);
                size--;
                return removed;
            }

            @Override
            public E set(int index, E element)
            {
                check(index);
                return AbstractTypedList.this.set(offset + index, element);
            }
        }
    }
    
    /**
     * TypedCollection is a generically typed collection that delegates to an 
     * underlying wildcard collection. All members of the delegate collection
     * must be assignable to the generic type specified at the time of 
     * construction.  
     * 
     * <p>Changes to this collection are reflected in the delegate
     * and vice versa.  Restrictions on concurrent modification of the
     * delegate apply to instances of this class as well.   In particular,
     * if the delegate must be externally synchronized before modification, 
     * you must arrange to do this outside the scope of TypedCollection.
     * 
     * <p>Efficiency of operations is equivalent to the deletegate's
     * efficiency plus the constant overhead of calls to the delegate and
     * casting operations. 
     */
    private static final class TypedCollection<E> 
        extends AbstractTypedCollection<E>
    {
        /**
         * Constructs a new TypedCollection with the given type which delegates
         * to the given collection.  All existing members of <code>c</code> 
         * must be assignable to a variable of type <code>cls</code>.
         * 
         * @param cls collection element type
         * @param c delegate collection
         */
        private TypedCollection(Class<E> cls, Collection<?> c)
        {
            super(cls, c);
        }
    }
    
    /**
     * TypedSet is identical to {@link TypedCollection} except that it
     * inherits from the {@link Set} interface.
     */
    private static final class TypedSet<E>  
        extends AbstractTypedCollection<E>
        implements Set<E>
    {
        /**
         * Constructs a new TypedSet with the given type which delegates
         * to the given set.  All existing members of <code>s</code> 
         * must be assignable to a variable of type <code>cls</code>.
         * 
         * @param cls collection element type
         * @param s delegate set
         */
        private TypedSet(Class<E> cls, Set<?> s)
        {
            super(cls, s);
        }
    }

    private static class AbstractTypedCollection<E> implements Collection<E>
    {
        protected final Collection<?> collection;
        protected final Class<E> type;

        protected AbstractTypedCollection(Class<E> cls, Collection<?> c)
        {
            this.collection = c;
            this.type = cls;
        }
        
        @SuppressWarnings("unchecked")
        public boolean add(E elem)
        {
            return ((Collection<Object>)collection).add(elem);
        }
    
        public Iterator<E> iterator()
        {
            return new TypedIterator();
        }
    
        public int size()
        {
            return collection.size();
        }

        @SuppressWarnings("unchecked")
        public boolean addAll(Collection<? extends E> c)
        {
            return ((Collection<Object>)collection).addAll(c);
        }

        public void clear()
        {
            collection.clear();
        }

        public boolean contains(Object o)
        {
            return collection.contains(o);
        }

        public boolean containsAll(Collection<?> c)
        {
            return collection.containsAll(c);
        }

        public boolean isEmpty()
        {
            return collection.isEmpty();
        }

        public boolean remove(Object o)
        {
            return collection.remove(o);
        }

        public boolean removeAll(Collection<?> c)
        {
            return collection.removeAll(c);
        }

        public boolean retainAll(Collection<?> c)
        {
            return collection.retainAll(c);
        }

        public Object[] toArray()
        {
            return collection.toArray();
        }

        public <T> T[] toArray(T[] a)
        {
            return collection.toArray(a);
        }
        
        public boolean equals(Object o)
        {
            return collection.equals(o);
        }
        
        public int hashCode()
        {
            return collection.hashCode();
        }
        
        public String toString()
        {
            return collection.toString();
        }
        
        private class TypedIterator implements Iterator<E>
        {
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
        }
    }
}

// End GenericCollections.java
