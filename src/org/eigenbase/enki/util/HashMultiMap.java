/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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
 * HashMultiMap implements a {@link HashMap}-like structure from a given
 * key type to a set of value types.  The values associated with any key
 * may be restricted to unique values (e.g., insertion of a duplicate value
 * as defined by {@link Object#hashCode()} and {@link Object#equals(Object)}
 * does not modify the collection).  This collection does not support the
 * removal of values.
 * 
 * @author Stephan Zuercher
 */
public class HashMultiMap<K, V>
{
    private final HashMap<K, Collection<V>> map;
    private final boolean unique;
    private int size;
    
    public HashMultiMap()
    {
        this.map = new HashMap<K, Collection<V>>();
        this.unique = false;
        this.size = 0;
    }
    
    public HashMultiMap(int keyCapacity)
    {
        this.map = new HashMap<K, Collection<V>>(keyCapacity);
        this.unique = false;
        this.size = 0;
    }
    
    public HashMultiMap(int keyCapacity, float loadFactor)
    {
        this.map = new HashMap<K, Collection<V>>(keyCapacity, loadFactor);
        this.unique = false;
        this.size = 0;
    }
    
    public HashMultiMap(boolean unique)
    {
        this.map = new HashMap<K, Collection<V>>();
        this.unique = unique;
        this.size = 0;
    }
    
    public HashMultiMap(boolean unique, int keyCapacity)
    {
        this.map = new HashMap<K, Collection<V>>(keyCapacity);
        this.unique = unique;
        this.size = 0;
    }
    
    public HashMultiMap(boolean unique, int keyCapacity, float loadFactor)
    {
        this.map = new HashMap<K, Collection<V>>(keyCapacity, loadFactor);
        this.unique = unique;
        this.size = 0;
    }
    
    public Collection<V> getValues(K key)
    {
        return map.get(key);
    }
    
    public void put(K key, V value)
    {
        Collection<V> values = create(key);
        
        values.add(value);
        size++;
    }
    
    public void putValues(K key, List<V> values)
    {
        Collection<V> existingValues = create(key);
        
        existingValues.addAll(values);
        size += values.size();
    }

    private Collection<V> create(K key)
    {
        Collection<V> values = map.get(key);
        if (values == null) {
            if (unique) {
                values = new HashSet<V>();
            } else {
                values = new ArrayList<V>();
            }
            
            map.put(key, values);
        }
        
        return values;
    }
    
    public boolean contains(K key, V value)
    {
        Collection<V> values = getValues(key);
        if (values == null) {
            return false;
        }
        
        return values.contains(value);
    }
    
    public int size()
    {
        return size;
    }
    
    public boolean isEmpty()
    {
        return size == 0;
    }
    
    public Set<K> keySet()
    {
        return map.keySet();
    }
    
    public Collection<V> values()
    {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator()
            {
                return new Iterator<V>() {
                    private final int expectedSize = size;
                    private final Iterator<K> keyIter = keySet().iterator();
                    private K key;
                    private Iterator<V> valueIter;
                    
                    public boolean hasNext()
                    {
                        if (expectedSize != size) {
                            throw new ConcurrentModificationException();
                        }
                        
                        return 
                            keyIter.hasNext() || 
                            (valueIter != null && valueIter.hasNext());
                    }

                    public V next()
                    {
                        if (expectedSize != size) {
                            throw new ConcurrentModificationException();
                        }
                        
                        if (valueIter != null) {
                            if (valueIter.hasNext()) {
                                return valueIter.next();
                            } else {
                                valueIter = null;
                            }
                        }
                        
                        if (keyIter.hasNext()) {
                            key = keyIter.next();
                            valueIter = map.get(key).iterator();
                            assert(valueIter.hasNext());
                            return valueIter.next();
                        }
                        
                        throw new NoSuchElementException();
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size()
            {
                return size;
            }            
        };
    }
    
    public Set<Map.Entry<K, V>> entrySet()
    {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public Iterator<Map.Entry<K, V>> iterator()
            {
                return new Iterator<Map.Entry<K, V>>() {
                    private final Iterator<K> keyIter = keySet().iterator();
                    private final int expectedSize = size;
                    private K key;
                    private Iterator<V> valueIter;
                    
                    public boolean hasNext()
                    {
                        if (expectedSize != size) {
                            throw new ConcurrentModificationException();
                        }
                        
                        return 
                            keyIter.hasNext() || 
                            (valueIter != null && valueIter.hasNext());
                    }

                    public Map.Entry<K, V> next()
                    {
                        if (expectedSize != size) {
                            throw new ConcurrentModificationException();
                        }
                        
                        if (valueIter != null) {
                            if (valueIter.hasNext()) {
                                return new Entry(key, valueIter.next());
                            } else {
                                valueIter = null;
                            }
                        }
                        
                        if (keyIter.hasNext()) {
                            key = keyIter.next();
                            valueIter = map.get(key).iterator();
                            assert(valueIter.hasNext());
                            return new Entry(key, valueIter.next());
                        }
                        
                        throw new NoSuchElementException();
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }

            @Override
            public int size()
            {
                return size;
            }
        };
    }
    
    private class Entry implements Map.Entry<K, V>
    {
        private final K key;
        private final V value;
        
        private Entry(K key, V value)
        {
            this.key = key;
            this.value = value;
        }
        public K getKey()
        {
            return key;
        }

        public V getValue()
        {
            return value;
        }

        public V setValue(V value)
        {
            throw new UnsupportedOperationException();
        }
    }
}

// End HashMultiMap.java
