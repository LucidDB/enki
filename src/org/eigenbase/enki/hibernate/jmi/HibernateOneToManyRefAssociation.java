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
 * HibernateOneToManyRefAssociation extends {@link HibernateRefAssociation}
 * to provide an implementation of {@link RefAssociation} for Hibernate-based
 * one-to-many associations.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyRefAssociation<E1 extends RefObject, E2 extends RefObject>
    extends HibernateRefAssociation
{
    private final Class<E1> end1Class;
    private final Class<E2> end2Class;
    private final boolean end1IsParent;
    
    protected HibernateOneToManyRefAssociation(
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

        assert(
            end1Multiplicity == Multiplicity.SINGLE ||
            end2Multiplicity == Multiplicity.SINGLE);
        assert(
            end1Multiplicity != Multiplicity.SINGLE ||
            end2Multiplicity != Multiplicity.SINGLE);
        
        this.end1Class = end1Class;
        this.end2Class = end2Class;
        
        this.end1IsParent = (end1Multiplicity == Multiplicity.SINGLE);
    }

    @Override
    protected Query getAllLinksQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + HibernateOneToManyAssociation.class.getName() +
                " where type = ?");
        
        return query;
    }
    
    protected boolean exists(E1 parent, E2 child)
    {
        return refLinkExists(parent, child);
    }

    @Override
    protected Query getExistsQuery(Session session)
    {
        // TODO: make named query
        Query query;
        if (end1IsParent) {
            query = session.createQuery(
                "from " + 
                HibernateOneToManyAssociation.class.getName() + 
                " where type = ? and parent = (?, ?) and (?, ?) in elements(children)");
        } else {
            query = session.createQuery(
                "from " + 
                HibernateOneToManyAssociation.class.getName() + 
                " where type = ? and (?, ?) in elements(children) and parent = (?, ?)");
        }
        return query;
    }

    protected <EX extends RefObject> EX getParentOf(
        RefObject child, Class<EX> cls)
    {
        if (end1IsParent) {
            assert(cls.equals(end1Class));
        } else {
            assert(cls.equals(end2Class));
        }
        
        Collection<? extends RefObject> c = super.query(!end1IsParent, child);
        assert(c.size() <= 1);
        if (c.isEmpty()) {
            return null;
        } else {
            return cls.cast(c.iterator().next());
        }
    }

    protected <EX extends RefObject> Collection<EX> getChildrenOf(
        RefObject parent, Class<EX> cls)
    {
        if (end1IsParent) {
            assert(cls.equals(end2Class)):
                "expected '" + end2Class.getName() + 
                "', got '" + cls.getName() + "'";
        } else {
            assert(cls.equals(end1Class)):
                "expected '" + end1Class.getName() + 
                "', got '" + cls.getName() + "'";
        }

        Collection<? extends RefObject> c = super.query(end1IsParent, parent);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<? extends RefObject>)c, cls);
        } else {
            return GenericCollections.asTypedCollection(c, cls);
        }
    }

    @Override
    protected Query getQueryQuery(Session session, boolean givenFirstEnd)
    {
        boolean givenParentEnd = (givenFirstEnd == end1IsParent);
        
        // TODO: make named queries
        Query query;
        if (givenParentEnd) {
            query = 
                session.createQuery(
                    "from " + HibernateOneToManyAssociation.class.getName() +
                    " where type = ? and parent = (?, ?)");
        } else {
            query = 
                session.createQuery(
                    "from " + HibernateOneToManyAssociation.class.getName() +
                    " where type = ? and (?, ?) in elements(children)");
        }
        return query;
    }
    
    @Override
    protected Collection<? extends RefObject> toRefObjectCollection(
        List<? extends HibernateAssociation> queryResult,
        boolean returnFirstEnd)
    {
        assert(queryResult.size() <= 1);
        
        if (queryResult.isEmpty()) {
            return Collections.emptySet();
        }

        HibernateAssociation assoc = queryResult.get(0);
        if (returnFirstEnd) {
            RefAssociationLink link = assoc.linkIterator().next();
            
            return Collections.singleton(link.refFirstEnd());
        }
        
        return new QueryResultCollection(assoc.getLinks(), returnFirstEnd);
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

    public boolean add(E1 end1, E2 end2)
    {
        HibernateAssociable associableParent;
        HibernateAssociable associableChild;
        
        if (end1IsParent) {
            associableParent = (HibernateAssociable)end1;
            associableChild = (HibernateAssociable)end2;
        } else {
            associableParent = (HibernateAssociable)end2;
            associableChild = (HibernateAssociable)end1;
        }
        
        return associableParent.getAssociation(type, true).add(
            associableParent, associableChild);
    }

    public boolean remove(E1 end1, E2 end2)
    {
        HibernateAssociable associableParent;
        HibernateAssociable associableChild;
        
        if (end1IsParent) {
            associableParent = (HibernateAssociable)end1;
            associableChild = (HibernateAssociable)end2;
        } else {
            associableParent = (HibernateAssociable)end2;
            associableChild = (HibernateAssociable)end1;
        }
        
        return associableParent.getAssociation(type, true).remove(
            associableParent, associableChild);
    }
}

// End HibernateOneToManyRefAssociation.java
