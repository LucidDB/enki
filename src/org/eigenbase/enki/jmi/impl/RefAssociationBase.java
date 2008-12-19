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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

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
    private final EnkiMDRepository repos;

    private final Set<RefAssociationLinkImpl> links;
    
    private final Map<RefObject, Collection<RefAssociationLinkImpl>> firstToSecondMap;
    private final Map<RefObject, Collection<RefAssociationLinkImpl>> secondToFirstMap;
    
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
        this.repos = getCurrentInitializer().getRepository();

        this.links = new HashSet<RefAssociationLinkImpl>();
        this.firstToSecondMap =
            new HashMap<RefObject, Collection<RefAssociationLinkImpl>>();
        this.secondToFirstMap = 
            new HashMap<RefObject, Collection<RefAssociationLinkImpl>>();

        this.end1Name = end1Name;
        this.end1Multiplicity = end1Multiplicity;
        this.end2Name = end2Name;
        this.end2Multiplicity = end2Multiplicity;
    }
    
    // Implement RefBaseObjectBase/RefBaseObject
    public RefPackage refImmediatePackage()
    {
        logJmi("refImmediatePackage");
        
        return container;
    }

    public boolean refAddLink(RefObject end1, RefObject end2)
    {
        logJmi("refAddLink");
        
        RefAssociationLinkImpl link = new RefAssociationLinkImpl(end1, end2);

        addToMap(link, true, -1);
        addToMap(link, false, -1);
        
        return links.add(link);
    }
    
    void addLink(
        RefObject end1, RefObject end2, int firstEndIndex, int secondEndIndex)
    {
        RefAssociationLinkImpl link = new RefAssociationLinkImpl(end1, end2);

        addToMap(link, true, firstEndIndex);
        addToMap(link, false, secondEndIndex);
        
        links.add(link);
    }

    @SuppressWarnings("unchecked")
    public Collection refAllLinks()
    {
        logJmi("refAllLinks");
        
        return Collections.unmodifiableCollection(
            new ArrayList<RefAssociationLinkImpl>(links));
    }

    public boolean refLinkExists(RefObject end1, RefObject end2)    
    {
        logJmi("refLinkExists");
        
        RefAssociationLinkImpl testLink = 
            new RefAssociationLinkImpl(end1, end2);
        
        return links.contains(testLink);
    }

    @SuppressWarnings("unchecked")
    public Collection refQuery(RefObject queryEnd, RefObject queryObject)
    {
        logJmi("refQuery(ByEnd)");

        Object queryEndName = queryEnd.refGetValue("name");
        boolean isFirst = end1Name.equals(queryEndName);
        if (!isFirst && !end2Name.equals(queryEndName)) {
            throw new InvalidCallException(this, queryEnd);
        }
        
        Collection<? extends RefObject> query = query(isFirst, queryObject);
        if (query == null) {
            throw new InvalidCallException(this, queryEnd);
        }
        return query;
    }

    @SuppressWarnings("unchecked")
    public Collection refQuery(String queryEndName, RefObject queryObject)
    {
        logJmi("refQuery(ByEndName)");
        
        boolean isFirst = end1Name.equals(queryEndName);
        if (!isFirst && !end2Name.equals(queryEndName)) {
            throw new InvalidNameException(queryEndName);
        }
        
        Collection<? extends RefObject> query = query(isFirst, queryObject);
        if (query == null) {
            throw new InvalidNameException(queryEndName);
        }
        return query;
    }

    public boolean refRemoveLink(RefObject end1, RefObject end2)
    {
        logJmi("refRemoveLink");
        
        throw new UnsupportedOperationException();
    }
    
    private void addToMap(
        RefAssociationLinkImpl link, boolean isFirstEnd, int index)
    {
        RefObject end1 = link.refFirstEnd();
        RefObject end2 = link.refSecondEnd();
        
        Map<RefObject, Collection<RefAssociationLinkImpl>> map;
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
        
        Collection<RefAssociationLinkImpl> referencedLinks = map.get(key);
        if (referencedLinks == null) {
            if (multiplicity.isOrdered()) {
                referencedLinks = new ArrayList<RefAssociationLinkImpl>();
            } else {
                referencedLinks = new HashSet<RefAssociationLinkImpl>();
            }
            
            map.put(key, referencedLinks);
        }
        
        if (multiplicity.isSingle()) {
            if (!referencedLinks.isEmpty() && 
                !referencedLinks.contains(link))
            {
                throw new WrongSizeException(
                    isFirstEnd ? link.refSecondEnd() : link.refFirstEnd());
            }
        } else if (multiplicity.isUnique() && referencedLinks.contains(link)) {
            return;
        }

        if (index < 0) {
            referencedLinks.add(link);
        } else {
            ((List<RefAssociationLinkImpl>)referencedLinks).add(index, link);
        }
    }
    
    protected Collection<? extends RefObject> query(
        boolean isFirstEnd, RefObject queryObject)
    {
        Map<RefObject, Collection<RefAssociationLinkImpl>> map;
        if (isFirstEnd) {
            map = firstToSecondMap;
        } else {
            map = secondToFirstMap;
        }
        
        Collection<RefAssociationLinkImpl> referencedLinks = 
            map.get(queryObject); 

        // isFirstEnd refers to the given end, so check the multiplicity of
        // the other end to see whether the result should be ordered.
        boolean isOrdered =
            (isFirstEnd && end2Multiplicity.isOrdered()) ||
            (!isFirstEnd && end1Multiplicity.isOrdered());

        if (referencedLinks == null) {
            if (isOrdered) {
                referencedLinks = new ArrayList<RefAssociationLinkImpl>();
            } else {
                referencedLinks = new HashSet<RefAssociationLinkImpl>();
            }
            map.put(queryObject, referencedLinks);
        }
        
        if (isOrdered) {
            return new AssocQueryList(
                this, 
                queryObject, 
                isFirstEnd, 
                (List<RefAssociationLinkImpl>)referencedLinks);
        } else {
            return new AssocQueryCollection(
                this,
                queryObject,
                isFirstEnd,
                referencedLinks);
        }
    }
    
    @Override
    protected void checkConstraints(
        List<JmiException> errors, boolean deepVerify)
    {
        Collection<RefAssociationLink> allLinks = 
            GenericCollections.asTypedCollection(
                refAllLinks(), RefAssociationLink.class);
        
        // check first ends
        for(RefAssociationLink link: allLinks) {
            Collection<? extends RefObject> secondEnds = 
                query(true, link.refFirstEnd());

            if ((secondEnds.isEmpty() && end2Multiplicity.getLower() > 0) ||
                secondEnds.size() < end2Multiplicity.getLower())
            {
                errors.add(
                    makeWrongSizeException(
                        findAssociationEnd(end1Name),
                        findAssociationEnd(end2Name), 
                        link.refFirstEnd()));
            }
        }
        
        // check second ends
        for(RefAssociationLink link: allLinks) {
            Collection<? extends RefObject> firstEnds = 
                query(false, link.refSecondEnd());
            
            if ((firstEnds.isEmpty() && end1Multiplicity.getLower() > 0) ||
                firstEnds.size() < end1Multiplicity.getLower())
            {
                errors.add(
                    makeWrongSizeException(
                        findAssociationEnd(end2Name), 
                        findAssociationEnd(end1Name),
                        link.refSecondEnd()));
            }
        }
    }
    
    private AssociationEnd findAssociationEnd(String endName)
    {
        Association assoc = (Association)refMetaObject();
        
        Collection<ModelElement> contents = 
            GenericCollections.asTypedCollection(
                assoc.getContents(), ModelElement.class);
        for(ModelElement elem: contents) {
            if (elem instanceof AssociationEnd) {
                if (elem.getName().equals(endName)) {
                    return (AssociationEnd)elem;
                }
            }
        }

        return null;
    }
    
    public static WrongSizeException makeWrongSizeException(
        AssociationEnd exposedEnd, AssociationEnd referencedEnd, RefObject obj)
    {
        return new WrongSizeException(
            referencedEnd, 
            "Not enough objects linked to " + obj.refMofId()
            + " at end '" + exposedEnd.getName() + "'.");
    }
    
    @Override
    public EnkiMDRepository getRepository()
    {
        return repos;
    }
}

// End RefAssociationBase.java
