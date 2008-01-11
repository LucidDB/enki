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

import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;

/**
 * HibernateManyToManyRefAssociation extends {@link HibernateRefAssociation}
 * to provide an implementation of {@link RefAssociation} for Hibernate-based
 * many-to-many associations.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyRefAssociation<E1 extends RefObject, E2 extends RefObject>
    extends HibernateRefAssociation
{
    private final Class<E1> end1Class;
    private final Class<E2> end2Class;
    
    protected HibernateManyToManyRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Class<E1> end1Class,
        Multiplicity end1Multiplicity,
        String end2Name,
        Class<E2> end2Class,
        Multiplicity end2Multiplicity)
    {
        super(
            container,
            type, 
            end1Name,
            end1Multiplicity, 
            end2Name, 
            end2Multiplicity);
     
        assert(end1Multiplicity != Multiplicity.SINGLE);
        assert(end2Multiplicity != Multiplicity.SINGLE);
        
        this.end1Class = end1Class;
        this.end2Class = end2Class;
    }

    @Override
    protected Query getAllLinksQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + HibernateManyToManyAssociation.class.getName() +
                " where type = ? and reversed = 0");
        
        return query;
    }

    protected boolean exists(E1 source, E2 target)
    {
        return super.refLinkExists(source, target);
    }

    @Override
    protected Query getExistsQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + 
                HibernateManyToManyAssociation.class.getName() +
                " where type = ? and source = (?, ?) and (?, ?) in elements(target)");
        
        return query;
    }

    protected Collection<E1> getSourceOf(E2 target)
    {
        Collection<? extends RefObject> c = super.query(false, target);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<? extends RefObject>)c, end1Class);
        } else {
            return GenericCollections.asTypedCollection(c, end1Class);
        }
    }

    protected Collection<E2> getTargetOf(E1 source)
    {
        Collection<? extends RefObject> c = super.query(true, source);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<? extends RefObject>)c, end2Class);
        } else {
            return GenericCollections.asTypedCollection(c, end2Class);
        }
    }

    @Override
    protected Class<? extends RefObject> getFirstEndType()
    {
        return end1Class;
    }
    
    @Override
    protected Class<? extends RefObject> getSecondEndType()
    {
        return end2Class;
    }
    
    @Override
    protected Query getQueryQuery(Session session, boolean givenFirstEnd)
    {
        // TODO: make named queries
        Query query;
        if (givenFirstEnd) {
            query = 
                session.createQuery(
                    "from " + HibernateManyToManyAssociation.class.getName() +
                    " where type = ? and reversed = 0 and source = (?, ?)");
        } else {
            query = 
                session.createQuery(
                    "from " + HibernateManyToManyAssociation.class.getName() +
                    " where type = ? and reversed = 1 and source = (?, ?)");
        }
        return query;
    }

    @Override
    protected Collection<? extends RefObject> toRefObjectCollection(
        List<? extends HibernateAssociation> queryResult,
        boolean returnFirstEnd)
    {
        if (queryResult.isEmpty()) {
            return Collections.emptySet();
        }

        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        for(HibernateAssociation assoc: queryResult) {
            links.addAll(assoc.getLinks());
        }
        
        return new QueryResultCollection(links, returnFirstEnd);
    }

    public boolean add(E1 end1, E2 end2)
    {
        HibernateAssociable associableEnd1 = (HibernateAssociable)end1;
        HibernateAssociable associableEnd2 = (HibernateAssociable)end2;
        
        return associableEnd1.getAssociation(type, true).add(
            associableEnd1, associableEnd2);
    }

    public boolean remove(E1 end1, E2 end2)
    {
        HibernateAssociable associableEnd1 = (HibernateAssociable)end1;
        HibernateAssociable associableEnd2 = (HibernateAssociable)end2;
        
        return associableEnd1.getAssociation(type, true).remove(
            associableEnd1, associableEnd2);
    }

}

// End HibernateManyToManyRefAssociation.java
