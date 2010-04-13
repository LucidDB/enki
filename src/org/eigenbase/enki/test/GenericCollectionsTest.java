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
package org.eigenbase.enki.test;

import java.lang.reflect.*;
import java.util.*;

import org.eigenbase.enki.util.*;
import org.junit.*;
import org.junit.runner.*;

/**
 * GenericCollectionsTest tests the collection wrappers returned by
 * {@link GenericCollections}.
 * 
 * @author Stephan Zuercher
 */
@RunWith(LoggingTestRunner.class)
public class GenericCollectionsTest
{
    @Test
    public void testSetWrapper()
    {
        Collection<?> delegate = 
            makeCollection(new HashSet<String>(), 123456L);

        Collection<String> expected = 
            makeCollection(new HashSet<String>(), 123456L);
        
        Collection<String> actual = 
            GenericCollections.asTypedCollection(delegate, String.class);
        
        testCollection(expected, actual);
    }
    
    @Test
    public void testCollectionWrapper()
    {
        Collection<String> temp = makeCollection(new Bag<String>(), 0xbeefL);
        temp.add("duplicate");
        temp.add("duplicate");
        Collection<?> delegate = temp;
        
        Collection<String> expected = 
            makeCollection(new Bag<String>(), 0xbeefL);
        expected.add("duplicate");
        expected.add("duplicate");

        Collection<String> actual = 
            GenericCollections.asTypedCollection(delegate, String.class);
        
        testCollection(expected, actual);        
    }
    
    @Test
    public void testListWrapper()
    {
        List<?> delegate = makeList(new LinkedList<String>(), 0xabadcafe);

        List<String> expected = makeList(new LinkedList<String>(), 0xabadcafe);
        
        List<String> actual = 
            GenericCollections.asTypedList(delegate, String.class);

        testCollection(expected, actual);

        // reset lists
        delegate = makeList(new LinkedList<String>(), 0xabadcafe);
        expected = makeList(new LinkedList<String>(), 0xabadcafe);
        actual = GenericCollections.asTypedList(delegate, String.class);

        testList(expected, actual);
    }
    
    @Test
    public void testRandomAccessListWrapper()
    {
        List<?> delegate = makeList(new ArrayList<String>(), 0xabadcafe);

        List<String> expected = makeList(new ArrayList<String>(), 0xabadcafe);
        
        List<String> actual = 
            GenericCollections.asTypedList(delegate, String.class);

        testCollection(expected, actual);

        // reset lists
        delegate = makeList(new ArrayList<String>(), 0xabadcafe);
        expected = makeList(new ArrayList<String>(), 0xabadcafe);
        actual = GenericCollections.asTypedList(delegate, String.class);

        testList(expected, actual);
    }

    private void testCollection(Collection<String> expected, Collection<String> actual)
    {        
        Assert.assertEquals(expected instanceof Set, actual instanceof Set);
        Assert.assertEquals(expected, actual);
        
        expected.add("enki-test");
        actual.add("enki-test");
        Assert.assertEquals(expected, actual);
        
        expected.remove("enki-test");
        actual.remove("enki-test");
        Assert.assertEquals(expected, actual);
        
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertEquals(expected.isEmpty(), actual.isEmpty());
        
        String[] expectedStrArray = expected.toArray(new String[0]);
        String[] actualStrArray = actual.toArray(new String[0]);
        Assert.assertEquals(expectedStrArray, actualStrArray);
        
        int pick = expected.size() / 2;
        
        Iterator<String> expectedIter = expected.iterator();
        Iterator<String> actualIter = actual.iterator();
        while(pick-- > 0) {
            expectedIter.next();
            actualIter.next();
        }
        
        expectedIter.remove();
        actualIter.remove();
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertEquals(expected.isEmpty(), actual.isEmpty());        
        Assert.assertEquals(expected, actual);
        
        expected.clear();
        actual.clear();
        Assert.assertEquals(expected.size(), actual.size());
        Assert.assertEquals(expected.isEmpty(), actual.isEmpty());
        
        // Add some new elements
        makeCollection(expected, 999);
        makeCollection(actual, 999);
        Assert.assertEquals(expected, actual);

        expected.add("new-entry");
        actual.add("different-new-entry");
        if (expected.equals(actual)) {
            throw new AssertionError("collections should not match");
        }
        
        // Test for concurrent modification
        actualIter = actual.iterator();
        actualIter.next();
        actual.add("new-element-during-iteration");
        
        try {
            actualIter.next();
            Assert.fail("Expected ConcurrentModificationException");
        } catch(ConcurrentModificationException e) {
            // expected
        }
        
        expected.clear();
        actual.clear();
    }
    
    private void testList(List<String> expected, List<String> actual)
    {
        Assert.assertEquals(
            expected instanceof RandomAccess,
            actual instanceof RandomAccess);

        for(int i = 0; i < expected.size(); i++) {
            Assert.assertEquals(expected.get(i), actual.get(i));
        }
        
        ListIterator<String> expectedIter = 
            expected.listIterator(expected.size());
        ListIterator<String> actualIter =
            actual.listIterator(actual.size());
        while(expectedIter.hasPrevious() && actualIter.hasPrevious()) {
            Assert.assertEquals(
                expectedIter.previousIndex(), actualIter.previousIndex());
            Assert.assertEquals(
                expectedIter.previous(), actualIter.previous());
        }
        
        int pick = expected.size() / 2;
        expected.remove(pick);
        actual.remove(pick);
        Assert.assertEquals(expected, actual);

        String s = "new-entry";
        expected.add(pick, s);
        actual.add(pick, s);
        Assert.assertEquals(expected, actual);      
        
        expected.set(0, s);
        actual.set(0, s);
        Assert.assertEquals(expected, actual);      

        Assert.assertEquals(expected.indexOf(s), actual.indexOf(s));
        Assert.assertEquals(expected.lastIndexOf(s), actual.lastIndexOf(s));
        
        int from = 2;
        int to = expected.size() - 2;
        
        List<String> expectedSubList = expected.subList(from, to);
        List<String> actualSubList = actual.subList(from, to);
        Assert.assertEquals(expectedSubList, actualSubList);
        Assert.assertEquals(expectedSubList.size(), actualSubList.size());
    }
    
    private List<String> makeList(List<String> l, long seed)
    {
        makeCollection(l, seed);
        
        return l;
    }
    
    private Collection<String> makeCollection(Collection<String> c, long seed)
    {
        Random rng = new Random(seed);
        
        int size = rng.nextInt(6) + 5; // size in [5, 10]
        
        for(int i = 0; i < size; i++) {
            StringBuilder b = new StringBuilder();
            int len = rng.nextInt(11) + 10; // len in [10, 20]
            for(int j = 0; j < len; j++) {
                int n = rng.nextInt(10 + 26 + 26); // 0-10, A-Z, a-z
                
                if (n < 10) {
                    b.append('0' + n);
                    continue;
                }
                n -= 10;
                if (n < 26) {
                    b.append('A' + n);
                    continue;
                }
                n -= 26;
                b.append('a' + n);
            }
            
            String str = b.toString();
            if (c.contains(str)) {
                i--;
                continue;
            }
            
            c.add(str);
        }
        
        return c;
    }
    
    /**
    * Bag is an implementation of {@link Collection} that does not provide
    * {@link Set} semantics.  Instead it maintains a count of the number of
    * times a particular object is added and provides for iterating over
    * each instance.  That is, 
    * <pre>
    *   Bag<Integer> bag = new Bag<Integer>();
    *   bag.add(1);
    *   bag.add(2);
    *   bag.add(1);
    * </pre>
    * has a {@link #size()} of 3.  All elements with the same value are
    * returned consecutively from the bag's {@link #iterator()}, but the 
    * order of value is non-deterministic.
    */
    private static class Bag<E> implements Collection<E>
    {
        /**
         * Implements {@link Iterator} for a {@link Bag}.
         */
        private final class BagIterator
            implements Iterator<E>
        {
            Iterator<Map.Entry<E, Integer>> iter;
            private Map.Entry<E, Integer> next;
            private int n;
            private boolean retrieved;
            
            private int expectedModifications;

            private BagIterator()
            {
                iter = contents.entrySet().iterator();
                expectedModifications = modifications;
                next = null;
                n = -1;
                retrieved = false;
            }
            
            public boolean hasNext()
            {
                if (next == null) {
                    return iter.hasNext();
                } 
             
                if ((n + 1) < next.getValue()) {
                    return true;
                }
                
                return iter.hasNext();
            }

            public E next()
            {
                checkForConcurrentModification();

                if (next == null || (n + 1) >= next.getValue()) {
                    next = iter.next();
                    n = -1;
                }
                
                retrieved = true;
                n++;
                return next.getKey();
            }

            public void remove()
            {
                checkForConcurrentModification();
                
                if (next == null || n == -1 || !retrieved) {
                    throw new IllegalStateException();
                }
                
                int curr = next.getValue();
                next.setValue(curr - 1);
                
                // Keep number remaining the same.
                n--; 
                
                Bag.this.size--;
                Bag.this.modifications++;
                expectedModifications++;
                retrieved = false;                
            }

            private void checkForConcurrentModification()
            {
                if (expectedModifications != modifications) {
                    throw new ConcurrentModificationException();
                }
            }
        }

        private final Map<E, Integer> contents;
        private int size;
        private int modifications;
        
        public Bag()
        {
            this.contents = new HashMap<E, Integer>();
            this.size = 0;
            this.modifications = 0;
        }
        
        public Iterator<E> iterator()
        {
            return new BagIterator();
        }

        public int size()
        {
            return size;
        }

        public boolean add(E o)
        {
            if (contents.containsKey(o)) {
                int n = contents.get(o);
                contents.put(o, n + 1);
            } else {
                contents.put(o, 1);
            }
            size++;
            modifications++;
            return true;
        }

        public boolean addAll(Collection<? extends E> c)
        {
            int m = modifications;
            for(E e: c) {
                add(e);
            }
            
            return m != modifications;
        }

        public void clear()
        {
            contents.clear();
            size = 0;
            modifications++;
        }

        public boolean contains(Object o)
        {
            return contents.containsKey(o);
        }

        public boolean containsAll(Collection<?> c)
        {
            for(Object o: c) {
                if (!contains(o)) {
                    return false;
                }
            }
            
            return true;
        }

        public boolean isEmpty()
        {
            return size == 0;
        }

        @SuppressWarnings("unchecked")
        public boolean remove(Object o)
        {
            if (contents.containsKey(o)) {
                int n = contents.get(o) - 1;
                if (n > 0) {
                    contents.put((E)o, n);
                } else {
                    contents.remove(o);
                }
                size--;
                modifications++;
                return true;
            }
            
            return false;
        }

        public boolean removeAll(Collection<?> c)
        {
            int m = modifications;
            for(Object o: c) {
                while(remove(o));
            }
            
            return m != modifications;
        }

        public boolean retainAll(Collection<?> c)
        {
            int m = modifications;
            for(Iterator<E> iter = iterator(); iter.hasNext(); ) {
                E e = iter.next();
                
                if (!c.contains(e)) {
                    iter.remove();
                }
            }
            
            return m != modifications;
        }

        public Object[] toArray()
        {
            Object[] a = new Object[size];
            int i = 0;
            for(E e: this) {
                a[i++] = e;
            }
            return a;
        }

        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a)
        {
            if (a.length < size) {
                a = (T[])Array.newInstance(
                    a.getClass().getComponentType(), size);
            }

            Object[] result = a;
            int i = 0;
            for(E e: this) {
                result[i++] = e;
            }
            if (a.length > size) {
                a[size] = null;
            }
            return a;
        }
        
        public boolean equals(Object o)
        {
            if (!(o instanceof Collection)) {
                return false;
            }
            
            Collection<?> other = (Collection<?>)o;
            
            return containsAll(other) && other.containsAll(this);
        }
    }
}

// End GenericCollectionsTest.java
