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
package org.eigenbase.enki.hibernate.jmi;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;

/**
 * HibernateRefAssociation provides a Hibernate-based implementation of 
 * {@link RefAssociation}.
 *
 * @author Stephan Zuercher
 */
public abstract class HibernateRefAssociation
    extends RefAssociationBase
    implements RefAssociation
{
    protected final String type;

    protected HibernateRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Multiplicity end1Multiplicity,
        String end2Name,
        Multiplicity end2Multiplicity)
    {
        super(
            container, 
            end1Name,
            end1Multiplicity,
            end2Name,
            end2Multiplicity);
        
        this.type = type;
    }

    public Collection<?> refAllLinks()
    {
        Session session = HibernateMDRepository.getCurrentSession();
        
        Query query = getAllLinksQuery(session);
        query.setEntity(0, type);
        
        ArrayList<javax.jmi.reflect.RefAssociationLink> links = 
            new ArrayList<javax.jmi.reflect.RefAssociationLink>();
        List<?> list = query.list();
        for(HibernateAssociation assoc: 
                GenericCollections.asTypedList(
                    list, HibernateAssociation.class))
        {
            for(javax.jmi.reflect.RefAssociationLink link: assoc) {
                links.add(link);
            }
        }
        
        return links;
    }

    /**
     * Obtain a query to implement {@link #refAllLinks()} where the query 
     * parameter is the association type.
     * 
     * @param session session to create query in
     * @return Hibernate Query object as described above
     */
    protected abstract Query getAllLinksQuery(Session session);
    
    public boolean refLinkExists(RefObject end1, RefObject end2)
    {
        Session session = HibernateMDRepository.getCurrentSession();
        
        Query query = getExistsQuery(session);
        query.setEntity(0, type);
        query.setEntity(1, end1);
        query.setEntity(2, end2);
        
        return !query.list().isEmpty();
    }

    /**
     * Obtain a query to implement {@link #refLinkExists(RefObject, RefObject)}
     * where the query parameters are the association type, and the two ends
     * of the association (as {@link RefObject} instances).
     * 
     * @param session session to create query in
     * @return Hibernate Query object as described above
     */
    protected abstract Query getExistsQuery(Session session);
    
    @Override
    protected Collection<RefObject> query(
        boolean isFirstEnd, RefObject queryObject)
    {
        Session session = HibernateMDRepository.getCurrentSession();

        Query query = getQueryQuery(session, isFirstEnd);
        query.setString(0, type);
        query.setEntity(1, queryObject);
        
        return Collections.unmodifiableCollection(
            GenericCollections.asTypedCollection(
                query.list(), RefObject.class));
    }
    
    /**
     * Obtain a query to implement {@link #refQuery(RefObject, RefObject)} and
     * {@link #refQuery(String, RefObject)} where the query parameters are the 
     * association type and the given end of the association (as a 
     * {@link RefObject} instance).
     * 
     * @param session session to create query in
     * @return Hibernate Query object as described above
     */
    protected abstract Query getQueryQuery(
        Session session, boolean isFirstEnd);

    public boolean refAddLink(RefObject end1, RefObject end2)
    {
        return ((HibernateAssociable)end1).getAssociation(type).add(
            (HibernateAssociable)end1, (HibernateAssociable)end2);
    }

    public boolean refRemoveLink(RefObject end1, RefObject end2)
    {
        return ((HibernateAssociable)end1).getAssociation(type).remove(
            (HibernateAssociable)end1, (HibernateAssociable)end2);
    }
}
