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
    public void addInitialTarget(HibernateAssociable target)
    {
        Set<Element> targets = getTarget();
        
        if (target== null) {
            targets = new HashSet<Element>();
            setTarget(targets);
        }
        
        targets.add(newElement(target));
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
        
        HibernateManyToManyLazyAssociationBase sourceAssoc =
            (HibernateManyToManyLazyAssociationBase)source.getAssociation(
                type, sourceIsFirstEnd);
        HibernateManyToManyLazyAssociationBase targetAssoc =
            (HibernateManyToManyLazyAssociationBase)target.getAssociation(
                type, targetIsFirstEnd);

        assert(equals(sourceAssoc, this) || equals(targetAssoc, this));

        Collection<Element> sourceAssocTargets = 
            sourceAssoc.getTargetElements();
        
        if (sourceAssocTargets.isEmpty()) {
            source.setAssociation(type, sourceIsFirstEnd, null);

            sourceAssoc.delete(getHibernateRepository(source));
        }
        
        Collection<Element> targetAssocTargets = 
            targetAssoc.getTargetElements();
        
        boolean removedFromTargetAssoc = 
            targetAssocTargets.remove(newElement(source));
        
        if (removedFromTargetAssoc) {
            if (targetAssocTargets.isEmpty()) {
                target.setAssociation(type, targetIsFirstEnd, null);
    
                targetAssoc.delete(getHibernateRepository(source));
            }
        }        
    }
    
    public final Kind getKind()
    {
        return Kind.MANY_TO_MANY;
    }
}

// End HibernateManyToManyLazyAssociation.java
