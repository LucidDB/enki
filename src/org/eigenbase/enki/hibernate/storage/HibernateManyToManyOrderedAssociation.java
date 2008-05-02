/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008-2008 The Eigenbase Project
// Copyright (C) 2008-2008 Disruptive Tech
// Copyright (C) 2008-2008 LucidEra, Inc.
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
public abstract class HibernateManyToManyOrderedAssociation
    extends HibernateManyToManyAssociationBase
    implements HibernateOrderedAssociation
{
    private List<HibernateAssociable> target;
    
    public HibernateManyToManyOrderedAssociation()
    {
        this.target = new ArrayList<HibernateAssociable>();
    }

    /*
     * N.B. Uniqueness is a MOF constraint and is NOT enforced by Netbeans
     * MDR.
     */
    public boolean getUnique()
    {
        return false;
    }
    
    public List<HibernateAssociable> getTarget()
    {
        return target;
    }

    public void setTarget(List<HibernateAssociable> target)
    {
        this.target = target;
    }
    
    @SuppressWarnings("unchecked")
    public <E> List<E> getTarget(Class<E> cls)
    {
        for(Object obj: target) {
            cls.cast(obj);
        }

        return (List<E>)target;
    }

    protected Collection<HibernateAssociable> getTargetCollection()
    {
        return getTarget();
    }
    
    public void add(
        int index, 
        HibernateAssociable newSource, 
        HibernateAssociable newTarget)
    {
        final String type = getType();

        HibernateManyToManyOrderedAssociation sourceAssoc =
            (HibernateManyToManyOrderedAssociation)newSource.getOrCreateAssociation(
                type, true);
        HibernateManyToManyOrderedAssociation targetAssoc =
            (HibernateManyToManyOrderedAssociation)newTarget.getOrCreateAssociation(
                type, false);

        boolean indexOnSourceAssoc = equals(sourceAssoc, this);
        boolean indexOnTargetAssoc = equals(targetAssoc, this);
        assert(indexOnSourceAssoc || indexOnTargetAssoc);
        
        List<HibernateAssociable> sourceAssocTargets = sourceAssoc.getTarget();
        if (!sourceAssoc.getUnique() ||
            !sourceAssocTargets.contains(newTarget))
        {
            if (indexOnSourceAssoc) {
                sourceAssocTargets.add(index, newTarget);
            } else {
                sourceAssocTargets.add(newTarget);
            }
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTarget();
        if (!targetAssoc.getUnique() ||
            !targetAssocTargets.contains(newSource))
        {
            if (indexOnTargetAssoc) {
                targetAssocTargets.add(index, newSource);
            } else {
                targetAssocTargets.add(newSource);
            }
        }
    }

    public boolean remove(
        int index, HibernateAssociable end1, HibernateAssociable end2)
    {
        HibernateAssociable source;
        HibernateAssociable target;
        if (getReversed()) {
            source = end2;
            target = end1;
        } else {
            source = end1;
            target = end2;
        }

        return removeInternal(source, target, index);
    }
    
    public Collection <HibernateAssociable> get(HibernateAssociable item)
    {
        return getOrdered(item);
    }
    
    public List<HibernateAssociable> getOrdered(HibernateAssociable item)
    {
        if (!equals(item, getSource())) {
            return Collections.emptyList();
        }
        
        return getTarget();
    }
}

// End HibernateManyToManyOrderedAssociation.java
