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

import org.eigenbase.enki.hibernate.jmi.*;

/**
 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyLazyAssociation
    extends HibernateManyToManyLazyAssociationBase
{
    private Set<Element> target;

    public HibernateManyToManyLazyAssociation()
    {
        super();
     
        this.target = new HashSet<Element>();
    }
    
    public Set<Element> getTarget()
    {
        return target;
    }
    
    public void setTarget(Set<Element> target)
    {
        this.target = target;
    }
    
    @Override
    public void addInitialTarget(HibernateAssociable newTarget)
    {
        Set<Element> targets = getTarget();
        
        if (newTarget== null) {
            targets = new HashSet<Element>();
            setTarget(targets);
        }
        
        targets.add(newElement(newTarget));
    }

    @Override
    public Collection<HibernateAssociable> getTargetCollection()
    {
        return new ElementCollection(getTarget());
    }

    @Override
    public Collection<Element> getTargetElements()
    {
        return getTarget();
    }

    @Override
    protected void emptyTargetElements()
    {
        setTarget(new HashSet<Element>());
    }

    @Override
    protected boolean getUnique()
    {
        return false;
    }

    public Collection<HibernateAssociable> get(HibernateAssociable item)
    {
        HibernateRefObject refObj = (HibernateRefObject)item;
        
        if (refObj.getMofId() == getSourceId()) {
            return getTargetCollection();
        } else {
            return Collections.emptySet();
        }
    }
    
    public void postRemove(HibernateAssociable end1, HibernateAssociable end2)
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

        final String type = getType();

        boolean targetIsFirstEnd = getReversed();
        boolean sourceIsFirstEnd = !targetIsFirstEnd;
        
        HibernateManyToManyLazyAssociationBase sourceAssoc =
            (HibernateManyToManyLazyAssociationBase)sourceEnd.getAssociation(
                type, sourceIsFirstEnd);
        HibernateManyToManyLazyAssociationBase targetAssoc =
            (HibernateManyToManyLazyAssociationBase)targetEnd.getAssociation(
                type, targetIsFirstEnd);

        assert(equals(sourceAssoc, this) || equals(targetAssoc, this));

        Collection<Element> sourceAssocTargets = 
            sourceAssoc.getTargetElements();
        
        if (sourceAssocTargets.isEmpty()) {
            sourceEnd.setAssociation(type, sourceIsFirstEnd, null);

            sourceAssoc.delete(getHibernateRepository(sourceEnd));
        }
        
        Collection<Element> targetAssocTargets = 
            targetAssoc.getTargetElements();
        
        boolean removedFromTargetAssoc = 
            targetAssocTargets.remove(newElement(sourceEnd));
        
        if (removedFromTargetAssoc) {
            if (targetAssocTargets.isEmpty()) {
                targetEnd.setAssociation(type, targetIsFirstEnd, null);
    
                targetAssoc.delete(getHibernateRepository(sourceEnd));
            }
        }        
    }
    
    public final Kind getKind()
    {
        return Kind.MANY_TO_MANY;
    }
}

// End HibernateManyToManyLazyAssociation.java
