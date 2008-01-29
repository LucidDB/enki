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

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateManyToManyAssociation extends HibernateAssociation to store
 * many-to-many associations. 

 * @author Stephan Zuercher
 */
public class HibernateManyToManyAssociation
    extends HibernateAssociation
{
    private boolean reversed;
    private HibernateAssociable source;
    private List<HibernateAssociable> target;
    
    public HibernateManyToManyAssociation()
    {
        this.target = new ArrayList<HibernateAssociable>();
    }

    public boolean getReversed()
    {
        return reversed;
    }
    
    public void setReversed(boolean reversed)
    {
        this.reversed = reversed;
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
    public boolean add(
        HibernateAssociable newSource, HibernateAssociable newTarget)
    {
        final String type = getType();

        HibernateManyToManyAssociation sourceAssoc =
            (HibernateManyToManyAssociation)newSource.getOrCreateAssociation(
                type, true);
        HibernateManyToManyAssociation targetAssoc =
            (HibernateManyToManyAssociation)newTarget.getOrCreateAssociation(
                type, false);
        
        boolean result = false;
        List<HibernateAssociable> sourceAssocTargets = sourceAssoc.getTarget();
        if (!sourceAssocTargets.contains(newTarget)) {
            sourceAssocTargets.add(newTarget);
            result = true;
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTarget();
        if (!targetAssocTargets.contains(newSource)) {
            targetAssocTargets.add(newSource);
            result = true;
        }
        
        return result;
    }

    @Override
    public void add(
        int index, 
        HibernateAssociable newSource, 
        HibernateAssociable newTarget)
    {
        final String type = getType();

        HibernateManyToManyAssociation sourceAssoc =
            (HibernateManyToManyAssociation)newSource.getOrCreateAssociation(
                type, true);
        HibernateManyToManyAssociation targetAssoc =
            (HibernateManyToManyAssociation)newTarget.getOrCreateAssociation(
                type, false);

        boolean indexOnSourceAssoc = equals(sourceAssoc, this);
        boolean indexOnTargetAssoc = equals(targetAssoc, this);
        assert(indexOnSourceAssoc || indexOnTargetAssoc);
        
        List<HibernateAssociable> sourceAssocTargets = sourceAssoc.getTarget();
        if (!sourceAssocTargets.contains(newTarget)) {
            if (indexOnSourceAssoc) {
                sourceAssocTargets.add(index, newTarget);
            } else {
                sourceAssocTargets.add(newTarget);
            }
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTarget();
        if (!targetAssocTargets.contains(newSource)) {
            if (indexOnTargetAssoc) {
                targetAssocTargets.add(index, newSource);
            } else {
                targetAssocTargets.add(newSource);
            }
        }
        
    }

    @Override
    public boolean remove(
        HibernateAssociable source, HibernateAssociable target)
    {
        final String type = getType();

        HibernateManyToManyAssociation sourceAssoc =
            (HibernateManyToManyAssociation)source.getAssociation(type, true);
        HibernateManyToManyAssociation targetAssoc =
            (HibernateManyToManyAssociation)target.getAssociation(type, false);

        assert(equals(sourceAssoc, this) || equals(targetAssoc, this));
        
        boolean result = false;
        List<HibernateAssociable> sourceAssocTargets = sourceAssoc.getTarget();
        if (sourceAssocTargets.remove(target)) {
            result = true;
            
            if (sourceAssocTargets.isEmpty()) {
                source.setAssociation(type, true, null);
    
                HibernateMDRepository.getCurrentSession().delete(sourceAssoc);
            }
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTarget();
        if (targetAssocTargets.remove(source)) {
            result = true;

            if (targetAssocTargets.isEmpty()) {
                target.setAssociation(type, false, null);
    
                HibernateMDRepository.getCurrentSession().delete(targetAssoc);
            }
        }
        
        return result;
    }
    
    @Override
    public void removeAll(HibernateAssociable item)
    {
        HibernateAssociable source = getSource();
        List<HibernateAssociable> targets = getTarget();

        if (!equals(item, source)) {
            assert(targets.contains(item));
            
            remove(source, item);
            return;
        }
        
        while(!targets.isEmpty()) {
            HibernateAssociable trg = targets.get(0);
            remove(item, trg);
        }
    }
    
    @Override
    public void clear(HibernateAssociable item)
    {
        assert(equals(getSource(), item));
        
        removeAll(item);
    }

    @Override
    protected List<HibernateAssociable> get(HibernateAssociable item)
    {
        if (!equals(item, getSource())) {
            return Collections.emptyList();
        }
        
        return getTarget();
    }

    @Override
    public Collection<RefAssociationLink> getLinks()
    {
        boolean reversed = getReversed();

        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        HibernateAssociable source = getSource();
        for(HibernateAssociable target: getTarget()) {
            RefAssociationLink link;
            if (reversed) {
                link = new RefAssociationLinkImpl(target, source);
            } else {
                link = new RefAssociationLinkImpl(source, target);
            }
            links.add(link);
        }
        return links;
    }

    @Override
    public Collection<? extends RefObject> query(boolean returnSecondEnd)
    {
        return Collections.unmodifiableCollection(getTarget());
    }
}

// End HibernateManyToManyAssociation.java
