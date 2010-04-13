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

import javax.jmi.reflect.*;

import org.netbeans.api.mdr.events.*;

/**
 * AttributeCollectionProxy extends {@link CollectionProxy} to generate MDR 
 * attribute events rather than MDR association events.
 * 
 * @author Stephan Zuercher
 */
public class AttributeCollectionProxy<E extends RefObject> 
    extends CollectionProxy<E>
{
    private final String attributeName;
    
    public AttributeCollectionProxy(
        HibernateAssociation assoc,
        HibernateAssociable source,
        boolean firstEnd,
        String attributeName,
        Class<E> cls)
    {
        super(assoc, source, firstEnd, null, cls);
        
        this.attributeName = attributeName;
    }

    public AttributeCollectionProxy(
        String type,
        HibernateAssociable source,
        boolean firstEnd, 
        String attributeName,
        Class<E> cls)
    {
        super(type, source, firstEnd, null, cls);
        
        this.attributeName = attributeName;
    }

    @Override
    protected void fireAddEvent(E e)
    {
        enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_ADD,
                attributeName,
                null,
                e,
                AttributeEvent.POSITION_NONE));
    }

    @Override
    protected void fireRemoveEvent(E e)
    {
        enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_REMOVE,
                attributeName,
                e,
                null,
                AttributeEvent.POSITION_NONE));
    }
    
    private void enqueueEvent(MDRChangeEvent event)
    {
        getHibernateRepository().enqueueEvent(event);
    }
}

// End AttributeCollectionProxy.java
