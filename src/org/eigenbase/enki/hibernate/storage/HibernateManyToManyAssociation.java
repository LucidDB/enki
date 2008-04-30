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
    
    public void postRemove(HibernateAssociable end1, HibernateAssociable end2)
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

        final String type = getType();

        boolean targetIsFirstEnd = getReversed();
        boolean sourceIsFirstEnd = !targetIsFirstEnd;
        
        HibernateManyToManyAssociationBase sourceAssoc =
            (HibernateManyToManyAssociationBase)source.getAssociation(
                type, sourceIsFirstEnd);
        HibernateManyToManyAssociationBase targetAssoc =
            (HibernateManyToManyAssociationBase)target.getAssociation(
                type, targetIsFirstEnd);

        boolean indexOnSourceAssoc = equals(sourceAssoc, this);
        boolean indexOnTargetAssoc = equals(targetAssoc, this);
        // This assertion also guarantees that either sourceAssoc or 
        // targetAssoc equals this.
        assert(indexOnSourceAssoc || indexOnTargetAssoc);

        Collection<HibernateAssociable> sourceAssocTargets = 
            sourceAssoc.getTargetCollection();
        
        if (sourceAssocTargets.isEmpty()) {
            source.setAssociation(type, sourceIsFirstEnd, null);

            sourceAssoc.delete(getHibernateRepository(source));
        }
        
        Collection<HibernateAssociable> targetAssocTargets = 
            targetAssoc.getTargetCollection();
        
        boolean removedFromTargetAssoc = targetAssocTargets.remove(source);
        
        if (removedFromTargetAssoc) {
            if (targetAssocTargets.isEmpty()) {
                target.setAssociation(type, targetIsFirstEnd, null);
    
                targetAssoc.delete(getHibernateRepository(source));
            }
        }        
    }
}

// End HibernateManyToManyAssociation.java
