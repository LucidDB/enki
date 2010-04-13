/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2008 The Eigenbase Project
// Copyright (C) 2008 SQLstream, Inc.
// Copyright (C) 2008 Dynamo BI Corporation
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

import org.eigenbase.enki.hibernate.jmi.*;

/**
 * HibernateManyToManyLazyOrderedAssociation extends 
 * HibernateManyToManyLazyAssociationBase to provide a base class that stores 
 * unordered, lazy, many-to-many associations.  It is extended per-model to 
 * provide separate storage for each model's associations. 

 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyLazyOrderedAssociation
    extends HibernateManyToManyLazyAssociationBase
    implements HibernateOrderedAssociation
{
    private List<Element> target;
    private ElementList targetWrapper;
    
    public HibernateManyToManyLazyOrderedAssociation()
    {
        super();
        
        this.target = new ArrayList<Element>();
    }

    public List<Element> getTarget()
    {
        return target;
    }
    
    public void setTarget(List<Element> target)
    {
        this.target = target;
    }
    
    @Override
    public void addInitialTarget(HibernateAssociable newTarget)
    {
        List<Element> targets = getTarget();
        
        if (newTarget == null) {
            targets = new ArrayList<Element>();
            setTarget(targets);
        }
        
        targets.add(newElement(newTarget));
    }

    @Override
    public Collection<HibernateAssociable> getTargetCollection()
    {
        return getTargetList();
    }

    protected List<HibernateAssociable> getTargetList()
    {
        List<Element> targetList = getTarget();
        
        if (targetWrapper == null || targetWrapper.elements != targetList) {
            targetWrapper = new ElementList(targetList);
        }

        return targetWrapper;
    }
    
    @Override
    public Collection<Element> getTargetElements()
    {
        return getTarget();
    }

    @Override
    protected void emptyTargetElements()
    {
        setTarget(new ArrayList<Element>());
    }

    /*
     * N.B. Uniqueness is a MOF constraint and is NOT enforced by Netbeans
     * MDR.
     */
    public boolean getUnique()
    {
        return false;
    }
    
    @SuppressWarnings("unchecked")
    public <E> List<E> getTarget(Class<E> cls)
    {
        for(Object obj: getTarget()) {
            cls.cast(obj);
        }

        return (List<E>)target;
    }

    public void add(
        int index, 
        HibernateAssociable newSource, 
        HibernateAssociable newTarget)
    {
        final String type = getType();

        HibernateManyToManyLazyOrderedAssociation sourceAssoc =
            (HibernateManyToManyLazyOrderedAssociation)newSource.getOrCreateAssociation(
                type, true);
        HibernateManyToManyLazyOrderedAssociation targetAssoc =
            (HibernateManyToManyLazyOrderedAssociation)newTarget.getOrCreateAssociation(
                type, false);

        boolean indexOnSourceAssoc = equals(sourceAssoc, this);
        boolean indexOnTargetAssoc = equals(targetAssoc, this);
        assert(indexOnSourceAssoc || indexOnTargetAssoc);
        
        List<HibernateAssociable> sourceAssocTargets = 
            sourceAssoc.getTargetList();
        if (!sourceAssoc.getUnique() ||
            !sourceAssocTargets.contains(newTarget))
        {
            if (indexOnSourceAssoc) {
                sourceAssocTargets.add(index, newTarget);
            } else {
                sourceAssocTargets.add(newTarget);
            }
        }
        
        List<HibernateAssociable> targetAssocTargets = targetAssoc.getTargetList();
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
        HibernateAssociable sourceEnd;
        HibernateAssociable targetEnd;
        if (getReversed()) {
            sourceEnd = end2;
            targetEnd = end1;
        } else {
            sourceEnd = end1;
            targetEnd = end2;
        }

        return removeInternal(sourceEnd, targetEnd, index);
    }
    
    public Collection <HibernateAssociable> get(HibernateAssociable item)
    {
        return getOrdered(item);
    }
    
    public List<HibernateAssociable> getOrdered(HibernateAssociable item)
    {
        long itemMofId = ((HibernateRefObject)item).getMofId();
        if (itemMofId != getSourceId()) {
            return Collections.emptyList();
        }
        
        return getTargetList();
    }
    
    public final Kind getKind()
    {
        return Kind.MANY_TO_MANY_ORDERED;
    }
}

// End HibernateManyToManyLazyOrderedAssociation.java
