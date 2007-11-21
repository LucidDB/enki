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

/**
 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyRefAssociation<L, R>
    extends HibernateRefAssociation
{
    protected boolean exists(L left, R right)
    {
        // TODO: implement
        return false;
    }

    protected Collection<L> getLeftOf(R right)
    {
        // TODO: implement
        return null;
    }

    protected Collection<R> getRightOf(L left)
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
