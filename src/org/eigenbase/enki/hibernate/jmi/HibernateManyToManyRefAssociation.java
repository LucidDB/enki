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
public abstract class HibernateManyToManyRefAssociation<S extends RefObject, T extends RefObject>
    extends HibernateRefAssociation
{
    private final Class<S> sourceClass;
    private final Class<T> targetClass;
    
    protected HibernateManyToManyRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Class<S> end1Class,
        Multiplicity end1Multiplicity,
        String end2Name,
        Class<T> end2Class,
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
        
        this.sourceClass = end1Class;
        this.targetClass = end2Class;
    }

    protected Query getAllLinksQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + HibernateManyToManyAssociation.class.getName() +
                " where type = ?");
        
        return query;
    }

    protected boolean exists(S source, T target)
    {
        return super.refLinkExists(source, target);
    }

    protected Query getExistsQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + 
                HibernateManyToManyAssociation.class.getName() +
                " type = ? and source = (?, ?) and (?, ?) in elements(target)");
        
        return query;
    }

    protected Collection<S> getSourceOf(T target)
    {
        Collection<RefObject> c = super.query(false, target);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<RefObject>)c, sourceClass);
        } else {
            return GenericCollections.asTypedCollection(c, sourceClass);
        }
    }

    protected Collection<T> getTargetOf(S source)
    {
        Collection<RefObject> c = super.query(true, source);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<RefObject>)c, targetClass);
        } else {
            return GenericCollections.asTypedCollection(c, targetClass);
        }
    }

    protected Class<? extends RefObject> getFirstEndType()
    {
        return sourceClass;
    }
    
    protected Class<? extends RefObject> getSecondEndType()
    {
        return targetClass;
    }
    
    protected Query getQueryQuery(Session session, boolean givenSourceEnd)
    {
        // TODO: make named queries
        Query query;
        if (givenSourceEnd) {
            query = 
                session.createQuery(
                    "select target " +
                    "from " + HibernateOneToOneAssociation.class.getName() +
                    " where type = ? and source = (?, ?)");
        } else {
            query = 
                session.createQuery(
                    "from " + 
                    HibernateOneToOneAssociation.class.getName() +
                    " where type = ? and (?, ?) in elements(target)");
        }
        return query;
    }

    public boolean add(S source, T target)
    {
        HibernateAssociable associableSource = (HibernateAssociable)source;
        HibernateAssociable associableTarget = (HibernateAssociable)target;
        
        return associableSource.getAssociation(type).add(
            associableSource, associableTarget);
    }

    public boolean remove(S source, T target)
    {
        HibernateAssociable associableSource = (HibernateAssociable)source;
        HibernateAssociable associableTarget = (HibernateAssociable)target;
        
        return associableSource.getAssociation(type).remove(
            associableSource, associableTarget);
    }

}

// End HibernateManyToManyRefAssociation.java
