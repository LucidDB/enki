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

import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateManyToManyRefAssociation extends {@link HibernateRefAssociation}
 * to provide an implementation of {@link RefAssociation} for Hibernate-based
 * many-to-many associations.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyRefAssociation<L, R>
    extends HibernateRefAssociation
{
    protected HibernateManyToManyRefAssociation(
        RefPackage container,
        String end1Name,
        Multiplicity end1Multiplicity,
        String end2Name,
        Multiplicity end2Multiplicity)
    {
        super(
            container, end1Name, end1Multiplicity, end2Name, end2Multiplicity);
     
        assert(end1Multiplicity != Multiplicity.SINGLE);
        assert(end2Multiplicity != Multiplicity.SINGLE);
    }

    protected boolean exists(L left, R right)
    {
        // TODO: implement
        return false;
    }

    protected List<L> getLeftOf(R right)
    {
        // TODO: implement
        return null;
    }

    protected List<R> getRightOf(L left)
    {
        // TODO: implement
        return null;
    }

    public boolean add(L left, R right)
    {
        // TODO: implement
        return false;
    }

    public boolean remove(L left, R right)
    {
        // TODO: implement
        return false;
    }

}

// End HibernateManyToManyRefAssociation.java
