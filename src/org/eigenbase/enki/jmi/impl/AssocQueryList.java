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
package org.eigenbase.enki.jmi.impl;

import java.util.*;

import javax.jmi.reflect.*;

/**
 * AssocQueryList implements a {@link List} of {@link RefObject} that 
 * automatically updates the {@link RefAssociationBase} object responsible
 * for managing an association.  Modifications to the collection or association
 * are reflected from one to the other. 
 * 
 * @author Stephan Zuercher
 */
public class AssocQueryList 
    extends AssocQueryCollection 
    implements List<RefObject>
{
    private final List<RefAssociationLinkImpl> linksList;
    
    public AssocQueryList(
        RefAssociationBase assoc,
        RefObject fixedEnd,
        boolean fixedIsFirstEnd,
        List<RefAssociationLinkImpl> links)
    {
        super(assoc, fixedEnd, fixedIsFirstEnd, links);

        this.linksList = links;
    }

    public void add(int index, RefObject element)
    {
        if (fixedIsFirstEnd) {
            assoc.addLink(fixedEnd, element, -1, index);
        } else {
            assoc.addLink(element, fixedEnd, index, -1);
        }
    }

    public boolean addAll(int index, Collection<? extends RefObject> c)
    {
        for(RefObject refObj: c) {
            add(index++, refObj);
        }
        
        return !c.isEmpty();
    }

    public RefObject get(int index)
    {
        if (fixedIsFirstEnd) {
            return linksList.get(index).refSecondEnd();
        } else {
            return linksList.get(index).refFirstEnd();            
        }
    }

    public int indexOf(Object o)
    {
        if (o instanceof RefObject) {
            RefObject refObj = (RefObject)o;
            if (fixedIsFirstEnd) {
                for(int index = 0; index < linksList.size(); index++) {
                    if (linksList.get(index).refSecondEnd().equals(refObj)) {
                        return index;
                    }
                }
            } else {
                for(int index = 0; index < linksList.size(); index++) {
                    if (linksList.get(index).refFirstEnd().equals(refObj)) {
                        return index;
                    }
                }
            }
        }
        
        return -1;
    }

    public int lastIndexOf(Object o)
    {
        if (o instanceof RefObject) {
            RefObject refObj = (RefObject)o;
            if (fixedIsFirstEnd) {
                for(int index = linksList.size() - 1; index >= 0; index--) {
                    if (linksList.get(index).refSecondEnd().equals(refObj)) {
                        return index;
                    }
                }
            } else {
                for(int index = linksList.size() - 1; index >= 0; index--) {
                    if (linksList.get(index).refFirstEnd().equals(refObj)) {
                        return index;
                    }
                }
            }
        }
        
        return -1;
    }

    public ListIterator<RefObject> listIterator()
    {
        return new ListIter();
    }

    public ListIterator<RefObject> listIterator(int index)
    {
        return new ListIter(index);
    }

    public RefObject remove(int index)
    {
        throw new UnsupportedOperationException();
    }

    public RefObject set(int index, RefObject element)
    {
        throw new UnsupportedOperationException();
    }

    public List<RefObject> subList(int fromIndex, int toIndex)
    {
        if (fromIndex < 0 || toIndex > size() || fromIndex > toIndex) {
            throw new IndexOutOfBoundsException();
        }
        
        return new SubList(fromIndex, toIndex);
    }
    
    /**
     * ListIter implements the result of {@link List#listIterator()} and 
     * {@link List#listIterator(int)} for AssocQueryList.Throws 
     * {@link UnsupportedOperationException} for list modification operations
     * other than {@link ListIterator#add(Object)}.
     */
    private class ListIter implements ListIterator<RefObject>
    {
        private ListIterator<RefAssociationLinkImpl> iter;
        
        private ListIter()
        {
            this.iter = linksList.listIterator();
        }
        
        private ListIter(int index)
        {
            this.iter = linksList.listIterator(index);            
        }

        public void add(RefObject o)
        {
            // Update the underlying list and then re-initialize the 
            // delegation ListIterator to point at the right spot.
            int index = iter.nextIndex();
            AssocQueryList.this.add(index, o);
            iter = linksList.listIterator(index + 1);
        }

        public boolean hasNext()
        {
            return iter.hasNext();
        }

        public boolean hasPrevious()
        {
            return iter.hasPrevious();
        }

        public RefObject next()
        {
            if (fixedIsFirstEnd) {
                return iter.next().refSecondEnd();
            } else {
                return iter.next().refFirstEnd();
            }
        }

        public int nextIndex()
        {
            return iter.nextIndex();
        }

        public RefObject previous()
        {
            if (fixedIsFirstEnd) {
                return iter.previous().refSecondEnd();
            } else {
                return iter.previous().refFirstEnd();
            }
        }

        public int previousIndex()
        {
            return iter.previousIndex();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }

        public void set(RefObject o)
        {
            throw new UnsupportedOperationException();
        }
    }
    
    /**
     * SubList implements the result of {@link List#subList(int, int)} for
     * AssocQueryList.  Depends on the underlying {@link AbstractList} 
     * implementation throwing {@link UnsupportedOperationException} for
     * list modification operations other than those that add new members.
     */
    private class SubList 
        extends AbstractList<RefObject> 
        implements RandomAccess
    {
        private final int fromIndex;
        private int toIndex;
        
        private SubList(int fromIndex, int toIndex)
        {
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
        }
        
        @Override
        public RefObject get(int index)
        {
            index = computeIndex(index);
            
            return AssocQueryList.this.get(index);
        }

        @Override
        public int size()
        {
            return toIndex - fromIndex;
        }
        
        @Override
        public void add(int index, RefObject o)
        {
            index = computeIndex(index);
            
            AssocQueryList.this.add(index, o);
            
            toIndex++;
        }

        private int computeIndex(int index)
        {
            index += fromIndex;
            if (index >= toIndex) {
                throw new IndexOutOfBoundsException();
            }
            return index;
        }
    }
}

// End AssocQueryList.java
