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
    // REVIEW: SWZ: 2008-01-22: Consider whether generic type info can be
    // pushed all the way down to RefAssociationBase, eliminating the need
    // to double-store these classes.
    private final Class<E1> end1GenericClass;
    private final Class<E2> end2GenericClass;
    
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
            end1Class,
            end1Multiplicity, 
            end2Name,
            end2Class,
            end2Multiplicity);
     
        assert(!end1Multiplicity.isSingle());
        assert(!end2Multiplicity.isSingle());
        
        this.end1GenericClass = end1Class;
        this.end2GenericClass = end2Class;
    }

    /**
     * Delegates to {@link #refLinkExists(RefObject, RefObject)}
     */
    protected boolean exists(E1 source, E2 target)
    {
        return super.refLinkExists(source, target);
    }

    protected Collection<E1> getSourceOf(E2 target)
    {
        HibernateAssociable t = (HibernateAssociable)target;
        HibernateAssociation assoc = t.getAssociation(type, false);
        
        if (assoc == null) {
            return Collections.emptyList();
        }

        if (end1Multiplicity.isOrdered()) {
            return new ListProxy<E1>(
                (HibernateOrderedAssociation)assoc,
                t,
                false,
                getAssociationIdentifier(),
                end1GenericClass);
        } else {
            return new CollectionProxy<E1>(
                assoc,
                (HibernateAssociable)t,
                false,
                getAssociationIdentifier(),
                end1GenericClass);
        }
    }

    protected Collection<E2> getTargetOf(E1 source)
    {
        HibernateAssociable s = (HibernateAssociable)source;
        HibernateAssociation assoc = s.getAssociation(type, true);
        
        if (assoc == null) {
            return Collections.emptyList();
        }
        
        if (end2Multiplicity.isOrdered()) {
            return new ListProxy<E2>(
                (HibernateOrderedAssociation)assoc,
                s,
                true,
                getAssociationIdentifier(),
                end2GenericClass);
        } else {
            return new CollectionProxy<E2>(
                assoc, s, true, getAssociationIdentifier(), end2GenericClass);
        }
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

// End HibernateManyToManyRefAssociation.java
