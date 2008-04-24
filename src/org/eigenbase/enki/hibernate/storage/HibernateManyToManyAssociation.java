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
 * HibernateManyToManyAssociation extends HibernateManyToManyAssociationBase
 * to provide a base class that stores unordered many-to-many associations.
 * It is extended per-model to provide separate storage for each model's 
 * associations. 

 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyAssociation
    extends HibernateManyToManyAssociationBase
{
    private Set<HibernateAssociable> target;
    
    public HibernateManyToManyAssociation()
    {
        this.target = new HashSet<HibernateAssociable>();
    }

    /*
     * N.B. Uniqueness is a MOF constraint and is NOT enforced by Netbeans
     * MDR.
     */
    public boolean getUnique()
    {
        return true;
    }
    
    public Set<HibernateAssociable> getTarget()
    {
        return target;
    }

    public void setTarget(Set<HibernateAssociable> target)
    {
        this.target = target;
    }
    
    @SuppressWarnings("unchecked")
    public <E> Set<E> getTarget(Class<E> cls)
    {
        for(Object obj: target) {
            cls.cast(obj);
        }

        return (Set<E>)target;
    }

    protected final Collection<HibernateAssociable> getTargetCollection()
    {
        return getTarget();
    }
    
    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        if (!equals(item, getSource())) {
            return Collections.emptyList();
        }
        
        return getTarget();
    }
}

// End HibernateManyToManyAssociation.java
