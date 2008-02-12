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

import javax.jmi.reflect.*;

import org.eigenbase.enki.hibernate.*;
import org.netbeans.api.mdr.events.*;

/**
 * AttributeListProxy extends {@link ListProxy} to generate MDR attribute
 * events rather than MDR association events.
 * 
 * @author Stephan Zuercher
 */
public class AttributeListProxy<E extends RefObject> extends ListProxy<E>
{
    private final String attributeName;
    
    public AttributeListProxy(
        HibernateAssociation assoc,
        HibernateAssociable source,
        boolean firstEnd,
        String attributeName,
        Class<E> cls)
    {
        super(assoc, source, firstEnd, null, cls);
        
        this.attributeName = attributeName;
    }

    public AttributeListProxy(
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
    protected void fireAddEvent(E e, int position)
    {
        enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_ADD,
                attributeName,
                null,
                e,
                position));
    }

    @Override
    protected void fireAddEvent(E e)
    {
        fireAddEvent(e, AttributeEvent.POSITION_NONE);
    }

    @Override
    protected void fireRemoveEvent(E e, int position)
    {
        enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_REMOVE,
                attributeName,
                e,
                null,
                position));
    }

    @Override
    protected void fireRemoveEvent(E e)
    {
        fireRemoveEvent(e, AttributeEvent.POSITION_NONE);
    }

    @Override
    protected void fireSetEvent(E oldE, E newE, int position)
    {
        enqueueEvent(
            new AttributeEvent(
                source,
                AttributeEvent.EVENT_ATTRIBUTE_SET,
                attributeName,
                oldE,
                newE,
                position));
    }
    
    private void enqueueEvent(MDRChangeEvent event)
    {
        HibernateMDRepository repos = 
            ((HibernateObject)source).getHibernateRepository();
        repos.enqueueEvent(event);
    }
}

// End AttributeListProxy.java
