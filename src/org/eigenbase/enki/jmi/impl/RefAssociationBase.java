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
 * RefAssociationBase implements {@link RefAssociation}.  
 * {@link RefAssociationBase} is designed for use with metamodels only. 
 * It stores, in memory, all links for a particular association instance and
 * does not allow links to be removed.
 * 
 * @author Stephan Zuercher
 */
public abstract class RefAssociationBase 
    extends RefBaseObjectBase
    implements RefAssociation
{
    private final RefPackage container;
    
    private final Set<RefAssociationLink> links;
    private final Map<RefObject, Collection<RefAssociationLink>> firstToSecondMap;
    private final Map<RefObject, Collection<RefAssociationLink>> secondToFirstMap;
    
    protected final String end1Name;
    protected final Multiplicity end1Multiplicity;
    protected final String end2Name;
    protected final Multiplicity end2Multiplicity;
    
    protected RefAssociationBase(
        RefPackage container, 
        String end1Name,
        Multiplicity end1Multiplicity,
        String end2Name,
        Multiplicity end2Multiplicity)
    {
        this.container = container;

        this.links = new HashSet<RefAssociationLink>();
        this.firstToSecondMap =
            new HashMap<RefObject, Collection<RefAssociationLink>>();
        this.secondToFirstMap = 
            new HashMap<RefObject, Collection<RefAssociationLink>>();

        this.end1Name = end1Name;
        this.end1Multiplicity = end1Multiplicity;
        this.end2Name = end2Name;
        this.end2Multiplicity = end2Multiplicity;
    }
    
    // Implement RefBaseObjectBase/RefBaseObject
    public RefPackage refImmediatePackage()
    {
        return container;
    }

    public boolean refAddLink(RefObject end1, RefObject end2)
    {
        RefAssociationLink link = new RefAssociationLink(end1, end2);

        addToMap(link, true, -1);
        addToMap(link, false, -1);
        
        return links.add(link);
    }
    
    void addLink(
        RefObject end1, RefObject end2, int firstEndIndex, int secondEndIndex)
    {
        RefAssociationLink link = new RefAssociationLink(end1, end2);

        addToMap(link, true, firstEndIndex);
        addToMap(link, false, secondEndIndex);
        
        links.add(link);
    }

    @SuppressWarnings("unchecked")
    public Collection refAllLinks()
    {
        ArrayList<RefAssociationLink> allLinks = 
            new ArrayList<RefAssociationLink>();
        for(Collection<RefAssociationLink> links: firstToSecondMap.values()) {
            allLinks.addAll(links);
        }
        return Collections.unmodifiableCollection(allLinks);
    }

    public boolean refLinkExists(RefObject end1, RefObject end2)    
    {
        RefAssociationLink testLink = new RefAssociationLink(end1, end2);
        
        return links.contains(testLink);
    }

    @SuppressWarnings("unchecked")
    public Collection refQuery(RefObject queryEnd, RefObject queryObject)
    {
        Object queryEndName = queryEnd.refGetValue("name");
        boolean isFirst = end1Name.equals(queryEndName);
        if (!isFirst && !end2Name.equals(queryEndName)) {
            throw new InvalidCallException(this, queryEnd);
        }
        
        Collection<RefObject> query = query(isFirst, queryObject);
        if (query == null) {
            throw new InvalidCallException(this, queryEnd);
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    public Collection refQuery(String queryEndName, RefObject queryObject)
    {
        boolean isFirst = end1Name.equals(queryEndName);
        if (!isFirst && !end2Name.equals(queryEndName)) {
            throw new InvalidNameException(queryEndName);
        }
        
        Collection<RefObject> query = query(isFirst, queryObject);
        if (query == null) {
            throw new InvalidNameException(queryEndName);
        }
        return query;
    }

    public boolean refRemoveLink(RefObject end1, RefObject end2)
    {
        throw new UnsupportedOperationException();
    }
    
    private void addToMap(
        RefAssociationLink link, boolean isFirstEnd, int index)
    {
        RefObject end1 = link.refFirstEnd();
        RefObject end2 = link.refSecondEnd();
        
        Map<RefObject, Collection<RefAssociationLink>> map;
        RefObject key;
        Multiplicity multiplicity;
        if (isFirstEnd) {
            map = firstToSecondMap;
            key = end1;
            multiplicity = end2Multiplicity;
        } else {
            map = secondToFirstMap;
            key = end2;
            multiplicity = end1Multiplicity;
        }
        
        if (!multiplicity.isOrdered()) {
            assert(index < 0): "Cannot used index with unordered collection";
            index = -1;
        }
        
        Collection<RefAssociationLink> links = map.get(key);
        if (links == null) {
            if (multiplicity.isOrdered()) {
                links = new ArrayList<RefAssociationLink>();
            } else {
                links = new HashSet<RefAssociationLink>();
            }
            
            map.put(key, links);
        }
        
        if (multiplicity.isSingle()) {
            if (!links.isEmpty() && !links.contains(link)) {
                throw new WrongSizeException(
                    isFirstEnd ? link.refSecondEnd() : link.refFirstEnd());
            }
        } else if (multiplicity.isUnique() && links.contains(link)) {
            return;
        }

        if (index < 0) {
            links.add(link);
        } else {
            ((List<RefAssociationLink>)links).add(index, link);
        }
    }
    
    protected Collection<RefObject> query(
        boolean isFirstEnd, RefObject queryObject)
    {
        Map<RefObject, Collection<RefAssociationLink>> map;
        if (isFirstEnd) {
            map = firstToSecondMap;
        } else {
            map = secondToFirstMap;
        }
        
        Collection<RefAssociationLink> links = map.get(queryObject); 

        // isFirstEnd refers to the given end, so check the multiplicity of
        // the other end to see whether the result should be ordered.
        boolean isOrdered =
            (isFirstEnd && end2Multiplicity.isOrdered()) ||
            (!isFirstEnd && end1Multiplicity.isOrdered());

        if (links == null) {
            if (isOrdered) {
                links = new ArrayList<RefAssociationLink>();
            } else {
                links = new HashSet<RefAssociationLink>();
            }
            map.put(queryObject, links);
        }
        
        if (isOrdered) {
            return new AssocQueryList(
                this, 
                queryObject, 
                isFirstEnd, 
                (List<RefAssociationLink>)links);
        } else {
            return new AssocQueryCollection(
                this,
                queryObject,
                isFirstEnd,
                links);
        }
    }
}

// End RefAssociationBase.java
