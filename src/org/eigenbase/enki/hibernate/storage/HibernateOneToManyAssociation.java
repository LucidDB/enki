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
package org.eigenbase.enki.hibernate.storage;

import java.util.*;

/**
 * HibernateOneToManyAssociation extends HibernateOneToManyAssociationBase to 
 * provide a base class that stores one-to-many associations.  It is extended 
 * per-model to provide separate storage for each model's associations.
 * Specifically, this class handles 0..1 to 0..*, 1 to 0..*, 0..1 to 1..*, and
 * 1 to 1..* associations where the many end is unordered.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateOneToManyAssociation
    extends HibernateOneToManyAssociationBase
{
    private Set<HibernateAssociable> children;
    
    public HibernateOneToManyAssociation()
    {
        super();
        this.children = new HashSet<HibernateAssociable>();
    }
    
    /*
     * N.B. Uniqueness is a MOF constraint and is NOT enforced by Netbeans
     * MDR.
     */
    public boolean getUnique()
    {
        return true;
    }
    
    public Set<HibernateAssociable> getChildren()
    {
        return children;
    }
    
    public void setChildren(Set<HibernateAssociable> children)
    {
        this.children = children;
    }

    @SuppressWarnings("unchecked")
    public <E> Set<E> getChildren(Class<E> cls)
    {
        for(Object child: children) {
            cls.cast(child);
        }

        return (Set<E>)children;
    }

    protected final Collection<HibernateAssociable> getCollection()
    {
        return getChildren();
    }
    
    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        if (!equals(item, getParent())) {
            return Collections.emptyList();
        }
        
        return getChildren();
    }
}

// End HibernateOneToManyAssociation.java
