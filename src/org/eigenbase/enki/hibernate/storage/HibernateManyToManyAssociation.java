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
 * HibernateManyToManyAssociation extends HibernateAssociation to store
 * many-to-many associations. 

 * @author Stephan Zuercher
 */
public class HibernateManyToManyAssociation
    extends HibernateAssociation
{
    private HibernateAssociable source;
    private List<HibernateAssociable> target;
    
    public HibernateManyToManyAssociation()
    {
        this.target = new ArrayList<HibernateAssociable>();
    }

    public HibernateAssociable getSource()
    {
        return source;
    }

    public void setSource(HibernateAssociable source)
    {
        this.source = source;
    }
    
    public <E> E getSource(Class<E> cls)
    {
        return cls.cast(source);
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

    @Override
    public void add(HibernateAssociable left, HibernateAssociable right)
    {
        final String type = getType();

        assert(equals(left, source) || equals(right, source));
        
        HibernateAssociable child = left;
        if (equals(left, source)) {
            child = right;
        }
        
        if (!target.contains(child)) {
            target.add(child);
        }
        
        HibernateManyToManyAssociation childAssoc = 
            (HibernateManyToManyAssociation)child.getOrCreateAssociation(type);
        
        if (!childAssoc.getTarget().contains(source)) {
            childAssoc.getTarget().add(source);
        }
    }

    @Override
    public void add(
        int index, HibernateAssociable left, HibernateAssociable right)
    {
        final String type = getType();

        assert(equals(left, source) || equals(right, source));
        
        HibernateAssociable child = left;
        if (equals(left, source)) {
            child = right;
        }
        
        if (!target.contains(child)) {
            target.add(index, child);
        }
        
        HibernateManyToManyAssociation childAssoc = 
            (HibernateManyToManyAssociation)child.getOrCreateAssociation(type);
        
        // TODO: SWZ: 11/15/07: Use index here, too?
        if (!childAssoc.getTarget().contains(source)) {
            childAssoc.getTarget().add(source);
        }
    }

    @Override
    public boolean remove(HibernateAssociable left, HibernateAssociable right)
    {
        final String type = getType();

        assert(equals(left, source) || equals(right, source));
        
        HibernateAssociable child = left;
        if (equals(left, source)) {
            child = right;
        }

        boolean targetResult = target.remove(child);
        if (target.isEmpty()) {
            source.setAssociation(type, null);

            // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete this?
            // Or does that happen auto-magically?
        }
        
        HibernateManyToManyAssociation childAssoc = 
            (HibernateManyToManyAssociation)child.getAssociation(type);
        assert(childAssoc != null);
        
        boolean sourceResult = childAssoc.getTarget().remove(source);
        if (childAssoc.getTarget().isEmpty()) {
            child.setAssociation(type, null);

            // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete 
            // childAssoc? Or does that happen auto-magically?
        }
        
        return targetResult || sourceResult;
    }
    
    public void clear(HibernateAssociable item)
    {
        if (equals(item, source)) {
            return;
        }
        
        final String type = getType();

        for(HibernateAssociable trg: target) {
            HibernateManyToManyAssociation trgAssoc =
                (HibernateManyToManyAssociation)trg.getAssociation(type);
            trgAssoc.remove(trg, source);
            if (trgAssoc.getTarget().isEmpty()) {
                trg.setAssociation(type, null);
            }
        }
        target.clear();
        source.setAssociation(type, null);

        // REVIEW: SWZ: 11/14/2007: get Hibernate session and delete this?
        // Or does that happen auto-magically?
    }
    
    @Override
    protected List<HibernateAssociable> get(HibernateAssociable item)
    {
        if (!equals(item, source)) {
            return Collections.emptyList();
        }
        
        return target;
    }
}

// End HibernateManyToManyAssociation.java
