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
public abstract class HibernateManyToManyRefAssociation<L extends RefObject, R extends RefObject>
    extends HibernateRefAssociation
{
    private final Class<L> leftClass;
    private final Class<R> rightClass;
    
    protected HibernateManyToManyRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Class<L> end1Class,
        Multiplicity end1Multiplicity,
        String end2Name,
        Class<R> end2Class,
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
        
        this.leftClass = end1Class;
        this.rightClass = end2Class;
    }

    protected Query getAllLinksQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + HibernateManyToManyAssociation.class.getName() +
                "where type = ?");
        
        return query;
    }

    protected boolean exists(L left, R right)
    {
        return super.refLinkExists(left, right);
    }

    protected Query getExistsQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + HibernateManyToManyAssociation.class.getName() +
                "where type = ? and source = ? and ? in elements(target)");
        
        return query;
    }

    protected Collection<L> getLeftOf(R right)
    {
        Collection<RefObject> c = super.query(false, right);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<RefObject>)c, leftClass);
        } else {
            return GenericCollections.asTypedCollection(c, leftClass);
        }
    }

    protected Collection<R> getRightOf(L left)
    {
        Collection<RefObject> c = super.query(true, left);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<RefObject>)c, rightClass);
        } else {
            return GenericCollections.asTypedCollection(c, rightClass);
        }
    }

    protected Query getQueryQuery(Session session, boolean givenLeftEnd)
    {
        // TODO: make named queries
        Query query;
        if (givenLeftEnd) {
            query = 
                session.createQuery(
                    "select target " +
                    "from " + HibernateOneToOneAssociation.class.getName() +
                    " where type = ? and source = ?");
        } else {
            query = 
                session.createQuery(
                    "select source " +
                    "from " + HibernateOneToOneAssociation.class.getName() +
                    " where type = ? and ? in elements(target)");
        }
        return query;
    }

    public boolean add(L left, R right)
    {
        HibernateAssociable associableLeft = (HibernateAssociable)left;
        HibernateAssociable associableRight = (HibernateAssociable)right;
        
        return associableLeft.getAssociation(type).add(
            associableLeft, associableRight);
    }

    public boolean remove(L left, R right)
    {
        HibernateAssociable associableLeft = (HibernateAssociable)left;
        HibernateAssociable associableRight = (HibernateAssociable)right;
        
        return associableLeft.getAssociation(type).remove(
            associableLeft, associableRight);
    }

}

// End HibernateManyToManyRefAssociation.java
