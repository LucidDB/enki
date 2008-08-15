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

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.codegen.*;
import org.eigenbase.enki.hibernate.jmi.*;
import org.eigenbase.enki.jmi.impl.*;

/**
 * HibernateManyToManyAssociationLazyBase is an abstract base class for lazy 
 * many-to-many association storage.
 * 
 * @author Stephan Zuercher
 */
public abstract class HibernateManyToManyLazyAssociationBase
    extends HibernateLazyAssociationBase
{
    private boolean reversed;
    
    private String sourceType;
    private long sourceId;
    
    public boolean getReversed()
    {
        return reversed;
    }
    
    public void setReversed(boolean reversed)
    {
        this.reversed = reversed;
    }
    
    public String getSourceType()
    {
        return sourceType;
    }
    
    public void setSourceType(String sourceType)
    {
        this.sourceType = sourceType;
    }
    
    public long getSourceId()
    {
        return sourceId;
    }
    
    public void setSourceId(long sourceId)
    {
        this.sourceId = sourceId;
    }
    
    public HibernateAssociable getSource()
    {
        String sourceType = getSourceType();
        if (sourceType == null) {
            return null;
        }
        
        return (HibernateAssociable)load(sourceType, getSourceId());
    }

    public <E> E getSource(Class<E> cls)
    {
        return cls.cast(getSource());
    }
    
    
    protected abstract boolean getUnique();
    public abstract Collection<Element> getTargetElements();
    protected abstract void emptyTargetElements();
    public abstract Collection<HibernateAssociable> getTargetCollection();
    
    public abstract void addInitialTarget(HibernateAssociable target);

    public void setInitialSource(HibernateAssociable parent)
    {
        HibernateRefObject refObj = (HibernateRefObject)parent;
        setSourceType(refObj.getClassIdentifier());
        setSourceId(refObj.getMofId());
    }
    
    public boolean add(
        HibernateAssociable newSource, HibernateAssociable newTarget)
    {
        final String type = getType();

        HibernateManyToManyLazyAssociationBase sourceAssoc =
            (HibernateManyToManyLazyAssociationBase)newSource.getOrCreateAssociation(
                type, true);
        HibernateManyToManyLazyAssociationBase targetAssoc =
            (HibernateManyToManyLazyAssociationBase)newTarget.getOrCreateAssociation(
                type, false);
        
        boolean result = false;
        Collection<HibernateAssociable> sourceAssocTargets = 
            sourceAssoc.getTargetCollection();
        if (!sourceAssoc.getUnique() || 
            !sourceAssocTargets.contains(newTarget))
        {
            sourceAssocTargets.add(newTarget);
            result = true;
        }
        
        Collection<HibernateAssociable> targetAssocTargets = 
            targetAssoc.getTargetCollection();
        if (!targetAssoc.getUnique() || 
            !targetAssocTargets.contains(newSource))
        {
            targetAssocTargets.add(newSource);
            result = true;
        }
        
        return result;
    }

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
    
    protected boolean removeInternal(
        HibernateAssociable source, HibernateAssociable target, int index) 
    {        
        final String type = getType();

        boolean targetIsFirstEnd = getReversed();
        boolean sourceIsFirstEnd = !targetIsFirstEnd;
        
        HibernateManyToManyLazyAssociationBase sourceAssoc =
            (HibernateManyToManyLazyAssociationBase)source.getAssociation(
                type, sourceIsFirstEnd);
        HibernateManyToManyLazyAssociationBase targetAssoc =
            (HibernateManyToManyLazyAssociationBase)target.getAssociation(
                type, targetIsFirstEnd);

        boolean indexOnSourceAssoc = equals(sourceAssoc, this);
        boolean indexOnTargetAssoc = equals(targetAssoc, this);
        // This assertion also guarantees that either sourceAssoc or 
        // targetAssoc equals this.
        assert(indexOnSourceAssoc || indexOnTargetAssoc);

        Element sourceElem = newElement(source);
        Element targetElem = newElement(target);
        
        boolean result = false;
        Collection<HibernateAssociable> sourceAssocTargets = 
            sourceAssoc.getTargetCollection();
        
        boolean removedFromSourceAssoc = false;
        if (indexOnSourceAssoc && index != -1) {
            HibernateAssociable removed = 
                ((List<HibernateAssociable>)sourceAssocTargets).remove(index);
            assert(
                ((HibernateRefObject)removed).getMofId() == 
                    targetElem.getChildId().longValue());
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
        
        Collection<HibernateAssociable> targetAssocTargets = 
            targetAssoc.getTargetCollection();
        
        boolean removedFromTargetAssoc = false;
        if (indexOnTargetAssoc && index != -1) {
            HibernateAssociable removed = 
                ((List<HibernateAssociable>)targetAssocTargets).remove(index);
            assert(
                ((HibernateRefObject)removed).getMofId() == 
                    sourceElem.getChildId().longValue());
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
    
    public void removeAll(
        HibernateAssociable item, boolean isFirstEnd, boolean cascadeDelete)
    {
        HibernateAssociable source = getSource();
        Collection<HibernateAssociable> targets = getTargetCollection();

        if (!equals(item, source)) {
            assert(targets.contains(item));
            
            removeInternal(source, item, -1);
            
            if (cascadeDelete) {
                source.refDelete();
            }
            return;
        }

        ArrayList<HibernateAssociable> trgs = 
            new ArrayList<HibernateAssociable>(targets);
        for(HibernateAssociable trg: trgs) {
            removeInternal(item, trg, -1);
            
            if (cascadeDelete) {
                trg.refDelete();
            }
        }
    }
    
    public void postRemove(HibernateAssociable end1, HibernateAssociable end2)
    {
        throw new UnsupportedOperationException();
    }

    public void clear(HibernateAssociable item)
    {
        assert(equals(getSource(), item));
        
        removeAll(item, !getReversed(), false);
    }

    public Collection<RefAssociationLink> getLinks()
    {
        boolean reversed = getReversed();

        ArrayList<RefAssociationLink> links = 
            new ArrayList<RefAssociationLink>();
        HibernateAssociable source = getSource();
        for(HibernateAssociable target: getTargetCollection()) {
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

    public Collection<? extends RefObject> query(boolean returnSecondEnd)
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
                return Collections.singleton(getSource());
            } else {
                return Collections.emptySet();
            }
        } else {
            return Collections.unmodifiableCollection(getTargetCollection());
        }
    }
    
    public String getCollectionName()
    {
        return HibernateMappingHandler.ASSOC_MANY_TO_MANY_TARGET_PROPERTY;
    }
}

// End HibernateManyToManyAssociationBase.java
