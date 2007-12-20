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
public abstract class HibernateOneToManyRefAssociation<P extends RefObject, C extends RefObject>
    extends HibernateRefAssociation
{
    private final Class<P> parentClass;
    private final Class<C> childClass;
    
    protected HibernateOneToManyRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Class<P> end1Class,
        String end2Name,
        Class<C> end2Class,
        Multiplicity end2Multiplicity)
    {
        super(
            container, 
            type,
            end1Name,
            Multiplicity.SINGLE, 
            end2Name,
            end2Multiplicity);
        
        assert(end2Multiplicity != Multiplicity.SINGLE);
        
        this.parentClass = end1Class;
        this.childClass = end2Class;
    }

    protected Query getAllLinksQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + HibernateOneToManyAssociation.class.getName() +
                " where type = ?");
        
        return query;
    }

    protected boolean exists(P parent, C child)
    {
        return refLinkExists(parent, child);
    }

    protected Query getExistsQuery(Session session)
    {
        // TODO: make named query
        Query query = 
            session.createQuery(
                "from " + 
                HibernateOneToManyAssociation.class.getName() + 
                " where type = ? and parent = (?, ?) and (?, ?) in elements(children)");
        
        return query;
    }

    protected P getParentOf(C child)
    {
        Collection<RefObject> c = super.query(false, child);
        assert(c.size() <= 1);
        if (c.isEmpty()) {
            return null;
        } else {
            return parentClass.cast(c.iterator().next());
        }
    }

    protected Collection<C> getChildrenOf(P parent)
    {
        Collection<RefObject> c = super.query(true, parent);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<RefObject>)c, childClass);
        } else {
            return GenericCollections.asTypedCollection(c, childClass);
        }
}

    protected Query getQueryQuery(Session session, boolean givenParentEnd)
    {
        // TODO: make named queries
        Query query;
        if (givenParentEnd) {
            query = 
                session.createQuery(
                    "select children " +
                    "from " + HibernateOneToOneAssociation.class.getName() +
                    " where type = ? and parent = (?, ?)");
        } else {
            query = 
                session.createQuery(
                    "select parent " +
                    "from " + HibernateOneToOneAssociation.class.getName() +
                    " where type = ? and (?, ?) in elements(children)");
        }
        return query;
    }

    protected Class<? extends RefObject> getFirstEndType()
    {
        return parentClass;
    }
    
    protected Class<? extends RefObject> getSecondEndType()
    {
        return childClass;
    }

    public boolean add(P parent, C child)
    {
        HibernateAssociable associableParent = (HibernateAssociable)parent;
        HibernateAssociable associableChild = (HibernateAssociable)child;
        
        return associableParent.getAssociation(type).add(
            associableParent, associableChild);
    }

    public boolean remove(P parent, C child)
    {
        HibernateAssociable associableParent = (HibernateAssociable)parent;
        HibernateAssociable associableChild = (HibernateAssociable)child;
        
        return associableParent.getAssociation(type).remove(
            associableParent, associableChild);
    }
}

// End HibernateOneToManyRefAssociation.java
