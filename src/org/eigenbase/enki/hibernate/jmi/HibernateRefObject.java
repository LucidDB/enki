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
package org.eigenbase.enki.hibernate.jmi;

import java.util.*;

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.netbeans.api.mdr.events.*;

/**
 * HibernateRefObject provides a Hibernate-based implementation of 
 * {@link RefObject}.
 *
 * @author Stephan Zuercher
 */
public abstract class HibernateRefObject
    extends HibernateObject
    implements RefObject
{
    @Override
    public RefClass refClass()
    {
        return HibernateRefClassRegistry.instance().findRefClass(
            getClassIdentifier());
    }
    
    @Override
    public void refDelete()
    {
        enqueueEvent(
            new InstanceEvent(
                this,
                InstanceEvent.EVENT_INSTANCE_DELETE,
                null,
                this));
        
        removeAssociations();
        
        super.delete();
    }
    
    @Override
    public RefFeatured refOutermostComposite()
    {
        RefFeatured immediateComposite = refImmediateComposite();
        if (immediateComposite == null) {
            return this;
        } else if (immediateComposite instanceof RefObject) {
            return ((RefObject)immediateComposite).refOutermostComposite();
        }
        
        // must be a RefClass
        return immediateComposite;
    }
    
    protected void associationSetSingle(
        String type, 
        String refAssocId, 
        boolean isExposedEndFirst,
        HibernateAssociable newValue)
    {
        HibernateRefAssociation refAssoc = null;
        if (refAssocId != null) {
            refAssoc = 
                HibernateRefAssociationRegistry.instance().findRefAssociation(
                    refAssocId);
        }

        HibernateAssociable me = (HibernateAssociable)this;
        
        // Get the existing association object
        HibernateAssociation assoc = 
            me.getAssociation(type, isExposedEndFirst);
        if (assoc == null && newValue != null) {
            // If none exists, get the new value's association.
            assoc = newValue.getAssociation(type, !isExposedEndFirst);
        }
        
        if (assoc == null) {
            if (newValue == null) {
                // User is clearing a non-existent association: do nothing
                return;
            }

            assoc = me.getOrCreateAssociation(type, isExposedEndFirst);
        }
        
        if (newValue != null) {
            HibernateAssociable end1, end2;
            if (isExposedEndFirst) {
                end1 = me;
                end2 = newValue;
            } else {
                end1 = newValue;
                end2 = me;
            }

            if (refAssoc != null) {
                refAssoc.fireAddEvent(true, end1, end2);
            }
            assoc.add(end1, end2);                
        } else {
            // remove any existing association
            if (refAssoc != null) {
                int index = 0;
                // This is not very intuitive, but it is how Netbeans behaves.
                for(RefObject end: assoc.query(isExposedEndFirst)) {
                    if (isExposedEndFirst) {
                        refAssoc.fireRemoveEvent(true, me, end, index++);
                    } else {
                        refAssoc.fireRemoveEvent(true, end, me, index++);
                    }
                } 
            }
            
            assoc.removeAll(me, false);
        }
    }
    
    protected void attributeSetSingle(
        String type, 
        String attribName, 
        boolean isExposedEndFirst,
        HibernateAssociable newValue)
    {
        HibernateAssociable me = (HibernateAssociable)this;
        
        boolean assocIsNew = false;
        
        // Get the existing association object
        HibernateAssociation assoc = 
            me.getAssociation(type, isExposedEndFirst);
        if (assoc == null && newValue != null) {
            // If none exists, get the new value's association.
            assoc = newValue.getAssociation(type, !isExposedEndFirst);
            assocIsNew = true;
        }
        
        if (assoc == null) {
            if (newValue == null) {
                // User is clearing a non-existent association: do nothing
                return;
            }
            
            assoc = me.getOrCreateAssociation(type, isExposedEndFirst);
            assocIsNew = true;
        }

        if (newValue != null) {
            RefObject oldValue = null;
            if (!assocIsNew) {
                Collection<? extends RefObject> otherEnd = 
                    assoc.query(isExposedEndFirst);
                oldValue = 
                    otherEnd.isEmpty() ? null : otherEnd.iterator().next();
            }
            
            fireAttributeSetEvent(attribName, oldValue, newValue);
            
            if (isExposedEndFirst) {
                assoc.add(me, newValue);
            } else {
                assoc.add(newValue, me);
            }
        } else {
            for(RefObject end: assoc.query(isExposedEndFirst)) {
                fireAttributeSetEvent(attribName, end, null);
            }
            
            // REVIEW: SWZ: 2008-02-07: Does setting a component attribute 
            // to null (e.g., phoneNumber.setAreaCode(null) cause the AreaCode 
            // to be deleted?  Here we assume no.
            assoc.removeAll(me, false);
        }
    }
    
    protected abstract void removeAssociations();
    
    protected abstract String getClassIdentifier();
    
    protected void fireAttributeSetEvent(
        String attribName, Object oldValue, Object newValue)
    {
        enqueueEvent(
            new AttributeEvent(
                this,
                AttributeEvent.EVENT_ATTRIBUTE_SET,
                attribName,
                oldValue,
                newValue,
                AttributeEvent.POSITION_NONE));
    }
    
    protected void fireAssociationRemoveAllEvents(
        String refAssocId,
        boolean isExposedEndFirst,
        HibernateAssociation assoc)
    {
        HibernateRefAssociation refAssoc = 
            HibernateRefAssociationRegistry.instance().findRefAssociation(
                refAssocId);
        
        List<? extends RefObject> otherEnds =
            assoc.query(isExposedEndFirst);
        for(int i = 0; i < otherEnds.size(); i++) {
            refAssoc.fireRemoveEvent(
                isExposedEndFirst, this, otherEnds.get(i), i);
        }
    }
    
    private void enqueueEvent(MDRChangeEvent event)
    {
        ((HibernateMDRepository)getRepository()).enqueueEvent(event);
    }
}
