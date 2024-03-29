/*
// $Id$
// Enki generates and implements the JMI and MDR APIs for MOF metamodels.
// Copyright (C) 2007 The Eigenbase Project
// Copyright (C) 2007 SQLstream, Inc.
// Copyright (C) 2007 Dynamo BI Corporation
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

import javax.jmi.model.*;
import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.eigenbase.enki.hibernate.storage.*;
import org.eigenbase.enki.mdr.*;
import org.eigenbase.enki.util.*;
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
    private final HibernateMDRepository repos;
    
    public HibernateRefObject()
    {
        this.repos = HibernateMDRepository.getCurrentRepository();
    }
    
    @Override
    public RefClass refClass()
    {
        logJmi("refClass");
        
        return getHibernateRepository().findRefClass(getClassIdentifier());
    }
    
    @Override
    public void refDelete()
    {
        logJmi("refDelete");
        
        repos.checkTransaction(true);
        
        enqueueEvent(
            new InstanceEvent(
                this,
                InstanceEvent.EVENT_INSTANCE_DELETE,
                null,
                this));
        
        removeAssociations();
        
        repos.recordObjectDeletion(this);
        
        super.delete();
    }
    
    protected void associationSetSingle(
        String type, 
        String refAssocId, 
        boolean isExposedEndFirst,
        HibernateAssociable newValue)
    {
        getHibernateRepository().checkTransaction(true);
        
        HibernateRefAssociation refAssoc = 
            getHibernateRepository().findRefAssociation(refAssocId);

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

            // Fixed end is always the first end.
            refAssoc.fireAddEvent(true, end1, end2);
            assoc.add(end1, end2);                
        } else {
            // remove any existing association
            int index = 0;
            // The fixed end is always the first end, which is not very 
            // intuitive, but it is how Netbeans behaves.  (Imagine a 1..*
            // assoc where the many end is the first end, now remove one
            // of the many-end elements from the association.  The event is 
            // generated with the fixed end set to the removed element.)
            for(RefObject end: assoc.query(isExposedEndFirst)) {
                if (isExposedEndFirst) {
                    refAssoc.fireRemoveEvent(true, me, end, index++);
                } else {
                    refAssoc.fireRemoveEvent(true, end, me, index++);
                }
            } 

            if (index > 0) {
                assoc.removeAll(me, isExposedEndFirst, false);
            }
        }
    }
    
    protected void attributeSetSingle(
        String type, 
        String attribName, 
        boolean isExposedEndFirst,
        HibernateAssociable newValue)
    {
        getHibernateRepository().checkTransaction(true);
        
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
            Collection<? extends RefObject> otherEnds =
                assoc.query(isExposedEndFirst);
            if (!otherEnds.isEmpty()) {
                for(RefObject otherEnd: otherEnds) {
                    fireAttributeSetEvent(attribName, otherEnd, null);
                }
            
                assoc.removeAll(me, isExposedEndFirst, false);
            }
        }
    }
    
    protected abstract void removeAssociations();
    
    /**
     * Returns a collection of associations which represent objects that 
     * compose this object.  This objects referred to by these associations
     * would return this from their {@link #refImmediateComposite} methods.
     */
    public abstract Collection<HibernateAssociation> getComposingAssociations();
    
    /**
     * Returns a collection of all associations this object participates in
     * that are not returned by {@link #getComposingAssociations()}.
     */
    public abstract Collection<HibernateAssociation> getNonComposingAssociations();
    
    public abstract String getClassIdentifier();
    
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
            getHibernateRepository().findRefAssociation(refAssocId);
        
        int i = 0;
        for(RefObject otherEnd: assoc.query(isExposedEndFirst)) {
            refAssoc.fireRemoveEvent(isExposedEndFirst, this, otherEnd, i++);            
        }
    }
    
    private void enqueueEvent(MDRChangeEvent event)
    {
        ((HibernateMDRepository)getRepository()).enqueueEvent(event);
    }
    
    /**
     * Helper method for {@link #checkConstraints(List, boolean)} 
     * implementations.  Finds the named AssociationEnd in the named
     * Association.
     * 
     * @param refAssocId the association's identifier
     * @param endName the association end's name
     * @return the AssociationEnd representing the association end or null if 
     *         not found
     */
    protected AssociationEnd findAssociationEnd(
        String refAssocId, String endName)
    {
        RefAssociation refAssoc = 
            getHibernateRepository().findRefAssociation(refAssocId);
        assert(refAssoc != null);
        
        Association assoc = (Association)refAssoc.refMetaObject();
                
        Collection<ModelElement> contents = 
            GenericCollections.asTypedCollection(
                assoc.getContents(), ModelElement.class);
        for(ModelElement elem: contents) {
            if (elem instanceof AssociationEnd) {
                if (elem.getName().equals(endName)) {
                    return (AssociationEnd)elem;
                }
            }
        }
        
        return null;
    }
    
    @Override
    public EnkiMDRepository getRepository()
    {
        return repos;
    }
    
    @Override
    public HibernateMDRepository getHibernateRepository()
    {
        return repos;
    }
}
