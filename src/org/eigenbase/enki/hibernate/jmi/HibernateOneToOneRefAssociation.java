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
import org.eigenbase.enki.jmi.impl.*;
import org.eigenbase.enki.util.*;
import org.hibernate.*;

/**
 * HibernateOneToOneRefAssociation extends HibernateRefAssociation to implement
 * {@link RefAssociation} for one-to-one associations.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToOneRefAssociation<E1 extends RefObject, E2 extends RefObject>
    extends HibernateRefAssociation
{
    private final Class<E1> end1Class;
    private final Class<E2> end2Class;
    
    protected HibernateOneToOneRefAssociation(
        RefPackage container,
        String type,
        String end1Name,
        Class<E1> end1Class,
        String end2Name,
        Class<E2> end2Class)
    {
        super(
            container,
            type,
            end1Name, 
            Multiplicity.SINGLE,
            end2Name, 
            Multiplicity.SINGLE);
        
        this.end1Class = end1Class;
        this.end2Class = end2Class;
    }

    protected boolean exists(E1 parent, E2 child)
    {
        return refLinkExists(parent, child);
    }
    
    protected E1 getParentOf(E2 child)
    {
        Collection<? extends RefObject> c = super.query(false, child);
        assert(c.size() <= 1);
        if (c.isEmpty()) {
            return null;
        } else {
            return end1Class.cast(c.iterator().next());
        }
    }

    protected E2 getChildOf(E1 parent)
    {
        Collection<? extends RefObject> c = super.query(true, parent);
        assert(c.size() <= 1);
        if (c.isEmpty()) {
            return null;
        } else {
            return end2Class.cast(c.iterator().next());
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
    
    protected boolean add(E1 end1, E2 end2)
    {
        return refAddLink(end1, end2);
    }

    protected boolean remove(E1 end1, E2 end2)
    {
        return refRemoveLink(end1, end2);
    }
}

// End HibernateOneToOneRefAssociation.java
