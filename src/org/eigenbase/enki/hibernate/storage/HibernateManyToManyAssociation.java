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

import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateManyToManyAssociation extends HibernateAssociation to provide a
 * base class that stores many-to-many associations.  It is extended per-model
 * to provide separate storage for each model's associations. 

 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyAssociation
    extends HibernateAssociation
{
    private boolean reversed;
    private boolean unique;
    
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
    
    public boolean getUnique()
    {
        return unique;
    }
    
    public void setUnique(boolean unique)
    {
        this.unique = unique;
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
        if (!sourceAssoc.getUnique() || 
            !sourceAssocTargets.contains(newTarget))
        {
            sourceAssocTargets.add(newTarget);
            result = true;
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTarget();
        if (!targetAssoc.getUnique() || 
            !targetAssocTargets.contains(newSource))
        {
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
        if (!targetAssoc.getUnique() &&
            !targetAssocTargets.contains(newSource))
        {
            if (indexOnTargetAssoc) {
                targetAssocTargets.add(index, newSource);
            } else {
                targetAssocTargets.add(newSource);
            }
        }
        
    }

    @Override
    public boolean remove(
        HibernateAssociable end1, HibernateAssociable end2)
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

        return removeInternal(source, target, -1);
    }
    
    @Override
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
    
    private boolean removeInternal(
        HibernateAssociable source, HibernateAssociable target, int index) 
    {        
        final String type = getType();

        boolean targetIsFirstEnd = getReversed();
        boolean sourceIsFirstEnd = !targetIsFirstEnd;
        
        HibernateManyToManyAssociation sourceAssoc =
            (HibernateManyToManyAssociation)source.getAssociation(
                type, sourceIsFirstEnd);
        HibernateManyToManyAssociation targetAssoc =
            (HibernateManyToManyAssociation)target.getAssociation(
                type, targetIsFirstEnd);

        boolean indexOnSourceAssoc = equals(sourceAssoc, this);
        boolean indexOnTargetAssoc = equals(targetAssoc, this);
        // This assertion also guarantees that either sourceAssoc or 
        // targetAssoc equals this.
        assert(indexOnSourceAssoc || indexOnTargetAssoc);

        boolean result = false;
        List<HibernateAssociable> sourceAssocTargets = sourceAssoc.getTarget();
        
        boolean removedFromSourceAssoc = false;
        if (indexOnSourceAssoc && index != -1) {
            HibernateAssociable removed = sourceAssocTargets.remove(index);
            assert(target.equals(removed));
            removedFromSourceAssoc = true;
        } else {
            removedFromSourceAssoc = sourceAssocTargets.remove(target);
        }
        if (removedFromSourceAssoc) {
            result = true;
            
            if (sourceAssocTargets.isEmpty()) {
                source.setAssociation(type, sourceIsFirstEnd, null);
    
                sourceAssoc.delete(getHibernateRepository(source));
            }
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTarget();
        
        boolean removedFromTargetAssoc = false;
        if (indexOnTargetAssoc && index != -1) {
            HibernateAssociable removed = targetAssocTargets.remove(index);
            assert(source.equals(removed));
            removedFromTargetAssoc = true;
        } else {
            removedFromTargetAssoc = targetAssocTargets.remove(source);
        }
        
        if (removedFromTargetAssoc) {
            result = true;

            if (targetAssocTargets.isEmpty()) {
                target.setAssociation(type, targetIsFirstEnd, null);
    
                targetAssoc.delete(getHibernateRepository(source));
            }
        }
        
        return result;
    }
    
    @Override
    public void removeAll(HibernateAssociable item, boolean cascadeDelete)
    {
        HibernateAssociable source = getSource();
        List<HibernateAssociable> targets = getTarget();

        if (!equals(item, source)) {
            assert(targets.contains(item));
            
            removeInternal(source, item, -1);
            
            if (cascadeDelete) {
                source.refDelete();
            }
            return;
        }
        
        while(!targets.isEmpty()) {
            HibernateAssociable trg = targets.get(0);
            removeInternal(item, trg, -1);
            
            if (cascadeDelete) {
                trg.refDelete();
            }
        }
    }
    
    @Override
    public void clear(HibernateAssociable item)
    {
        assert(equals(getSource(), item));
        
        removeAll(item, false);
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
    public List<? extends RefObject> query(boolean returnSecondEnd)
    {
        boolean reversed = getReversed();
        boolean getSource;
        if (reversed) {
            getSource = returnSecondEnd;
        } else {
            getSource = !returnSecondEnd;
        }

        if (getSource) {
            RefObject source = getSource();
            if (source != null) {
                return Collections.singletonList(getSource());
            } else {
                return Collections.emptyList();
            }
        } else {
            return Collections.unmodifiableList(getTarget());
        }
    }
}

// End HibernateManyToManyAssociation.java
