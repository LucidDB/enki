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
import java.util.concurrent.*;

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;

/**
 * RefAssociationBase implements {@link RefAssociation}.  {@link
 * RefAssociationBase} is designed for use with non-persistent models only.  It
 * stores, in memory, all links for a particular association instance.
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
    
    protected final String end1Name;
    protected final String end1Key;
    protected final Class<? extends RefObject> end1Class;
    protected final Multiplicity end1Multiplicity;
    protected final String end2Name;
    protected final String end2Key;
    protected final Class<? extends RefObject> end2Class;
    protected final Multiplicity end2Multiplicity;

    private final boolean isReposWeak;

    protected RefAssociationBase(
        RefPackage container, 
        String end1Name,
        Class<? extends RefObject> end1Class,
        Multiplicity end1Multiplicity,
        String end2Name,
        Class<? extends RefObject> end2Class,
        Multiplicity end2Multiplicity)
    {
        this.container = container;
        this.repos = getCurrentInitializer().getRepository();
        if (repos == null) {
            isReposWeak = false;
        } else {
            isReposWeak = repos.isWeak();
        }

        if (!isReposWeak) {
            this.links = Collections.<RefAssociationLinkImpl>newSetFromMap(
                new ConcurrentHashMap<RefAssociationLinkImpl, Boolean>());
        } else {
            this.links = Collections.<RefAssociationLinkImpl>emptySet();
        }

        this.end1Name = end1Name;
        this.end1Class = end1Class;
        this.end1Multiplicity = end1Multiplicity;
        this.end2Name = end2Name;
        this.end2Class = end2Class;
        this.end2Multiplicity = end2Multiplicity;

        end1Key = getClass().getName() + ":" + end1Name;
        end2Key = getClass().getName() + ":" + end2Name;
    }
    
    @Deprecated
    protected RefAssociationBase(
        RefPackage container, 
        String end1Name,
        Multiplicity end1Multiplicity,
        String end2Name,
        Multiplicity end2Multiplicity)
    {
        this(
            container, 
            end1Name,
            null,
            end1Multiplicity, 
            end2Name,
            null,
            end2Multiplicity);
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
        
        checkTypes(end1, end2);
        
        RefAssociationLinkImpl link = new RefAssociationLinkImpl(end1, end2);

        addToMap(link, true, -1);
        addToMap(link, false, -1);

        if (isReposWeak) {
            return true;
        } else {
            return links.add(link);
        }
    }
    
    void addLink(
        RefObject end1, RefObject end2, int firstEndIndex, int secondEndIndex)
    {
        RefAssociationLinkImpl link = new RefAssociationLinkImpl(end1, end2);

        addToMap(link, true, firstEndIndex);
        addToMap(link, false, secondEndIndex);

        if (isReposWeak) {
            links.add(link);
        }
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
        
        checkTypes(end1, end2);

        RefAssociationLinkImpl testLink = 
            new RefAssociationLinkImpl(end1, end2);

        if (isReposWeak) {
            Collection<RefAssociationLinkImpl> referencedLinks =
                getLinkCollection(end1, true);
            if (referencedLinks == null) {
                return false;
            }
            return referencedLinks.contains(testLink);
        } else {
            return links.contains(testLink);
        }
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
        
        checkTypes(end1, end2);

        RefAssociationLinkImpl link = new RefAssociationLinkImpl(end1, end2);

        if (removeFromMaps(link)) {
            return true;
        }

        if (isReposWeak) {
            return true;
        } else {
            return links.remove(link);
        }
    }
    
    private void addToMap(
        RefAssociationLinkImpl link, boolean isFirstEnd, int index)
    {
        RefObject key;
        Multiplicity multiplicity;
        if (isFirstEnd) {
            key = link.refFirstEnd();
            multiplicity = end2Multiplicity;
        } else {
            key = link.refSecondEnd();
            multiplicity = end1Multiplicity;
        }
        
        if (!multiplicity.isOrdered()) {
            assert(index < 0): "Cannot used index with unordered collection";
            index = -1;
        }
        
        Collection<RefAssociationLinkImpl> referencedLinks =
            getLinkCollection(key, isFirstEnd);

        if (referencedLinks == null) {
            if (multiplicity.isOrdered()) {
                referencedLinks = new ArrayList<RefAssociationLinkImpl>();
            } else {
                referencedLinks = new HashSet<RefAssociationLinkImpl>();
            }
            setLinkCollection(key, isFirstEnd, referencedLinks);
        }
        
        if (multiplicity.isSingle()) {
            if (!referencedLinks.isEmpty() && 
                !referencedLinks.contains(link))
            {
                throw new WrongSizeException(
                    isFirstEnd ? link.refSecondEnd() : link.refFirstEnd());
            }
        }

        if (index < 0) {
            referencedLinks.add(link);
        } else {
            ((List<RefAssociationLinkImpl>)referencedLinks).add(index, link);
        }
    }
    
    private boolean removeFromMaps(RefAssociationLinkImpl link)
    {
        RefObject end1 = link.refFirstEnd();
        RefObject end2 = link.refSecondEnd();

        boolean handleEnd1 = true;
        boolean handleEnd2 = true;
        if (end2Multiplicity.isOrdered() && end1Multiplicity.isSingle()) {
            List<RefAssociationLinkImpl> referencedLinks =
                (List<RefAssociationLinkImpl>) getLinkCollection(end1, true);

            boolean removed = referencedLinks.remove(link);
            if (!removed) {
                return false;
            }
            
            boolean additional = referencedLinks.lastIndexOf(link) >= 0;
            if (additional) {
                // Don't remove from the other direction or the link set
                return true;
            }
            
            handleEnd1 = false;
        } else if (end1Multiplicity.isOrdered() && end2Multiplicity.isSingle())
        {
            List<RefAssociationLinkImpl> referencedLinks = 
                (List<RefAssociationLinkImpl>) getLinkCollection(end2, false);

            boolean removed = referencedLinks.remove(link);
            if (!removed) {
                return false;
            }
            
            boolean additional = referencedLinks.lastIndexOf(link) >= 0;
            if (additional) {
                // Don't remove from the other direction or the link set
                return true;
            }
            
            handleEnd2 = false;
        }
        
        if (handleEnd1) {
            Collection<RefAssociationLinkImpl> referencedLinks =
                getLinkCollection(end1, true);

            referencedLinks.remove(link);
        }
        
        if (handleEnd2) {
            Collection<RefAssociationLinkImpl> referencedLinks = 
                getLinkCollection(end2, false);

            referencedLinks.remove(link);
        }

        return false;
    }
    
    protected Collection<? extends RefObject> query(
        boolean isFirstEnd, RefObject queryObject)
    {
        if (isFirstEnd) {
            checkFirstEndType(queryObject);
        } else {
            checkSecondEndType(queryObject);
        }
        
        Collection<RefAssociationLinkImpl> referencedLinks =
            getLinkCollection(queryObject, isFirstEnd);

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
            setLinkCollection(queryObject, isFirstEnd, referencedLinks);
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
    
    protected void checkTypes(RefObject end1, RefObject end2)
    {
        checkFirstEndType(end1);
        checkSecondEndType(end2);
    }

    protected void checkFirstEndType(RefObject end1)
    {
        if (!end1Class.isAssignableFrom(end1.getClass())) {
            throw new TypeMismatchException(end1Class, this, end1);
        }
    }    
    
    protected void checkSecondEndType(RefObject end2)
    {
        if (!end2Class.isAssignableFrom(end2.getClass())) {
            throw new TypeMismatchException(end2Class, this, end2);
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

    private Collection<RefAssociationLinkImpl> getLinkCollection(
        RefObject obj, boolean isFirstEnd)
    {
        String endKey = isFirstEnd ? end1Key : end2Key;
        return ((RefObjectBase) obj).getLinkCollection(endKey);
    }

    private void setLinkCollection(
        RefObject obj, boolean isFirstEnd,
        Collection<RefAssociationLinkImpl> links)
    {
        String endKey = isFirstEnd ? end1Key : end2Key;
        ((RefObjectBase) obj).setLinkCollection(endKey, links);
    }
}

// End RefAssociationBase.java
