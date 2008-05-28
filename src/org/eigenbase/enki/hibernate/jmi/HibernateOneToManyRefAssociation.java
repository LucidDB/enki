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
    /** Expected end 1 type. */
    private final Class<E1> end1Class;

    /** Expected end 1 type. */
    private final Class<E2> end2Class;

    /** If true end 1 is the single end.  If false, end 2 is the single end. */
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

        assert(end1Multiplicity.isSingle() || end2Multiplicity.isSingle());
        assert(!end1Multiplicity.isSingle() || !end2Multiplicity.isSingle());
        
        this.end1Class = end1Class;
        this.end2Class = end2Class;
        
        this.end1IsParent = end1Multiplicity.isSingle();
    }

    /**
     * Delegates to {@link #refLinkExists(RefObject, RefObject)}.
     */
    protected boolean exists(E1 parent, E2 child)
    {
        return refLinkExists(parent, child);
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

        HibernateAssociable p = (HibernateAssociable)parent;
        HibernateAssociationBase assoc = 
            (HibernateAssociationBase)p.getAssociation(type, end1IsParent);

        if (assoc == null) {
            return Collections.emptyList();
        }
       
        Collection<? extends RefObject> c = assoc.get(p);
        if (c instanceof List) {
            return GenericCollections.asTypedList(
                (List<? extends RefObject>)c, cls);
        } else {
            return GenericCollections.asTypedCollection(c, cls);
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
    
    /**
     * Delegates to {@link #refAddLink(RefObject, RefObject)}.
     */
    protected boolean add(E1 end1, E2 end2)
    {
        return refAddLink(end1, end2);
    }

    /**
     * Delegates to {@link #refRemoveLink(RefObject, RefObject)}.
     */
    protected boolean remove(E1 end1, E2 end2)
    {
        return refRemoveLink(end1, end2);
    }
}

// End HibernateOneToManyRefAssociation.java
