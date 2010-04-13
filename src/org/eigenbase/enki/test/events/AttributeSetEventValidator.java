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
package org.eigenbase.enki.test.events;

import javax.jmi.reflect.*;

import org.netbeans.api.mdr.events.*;

/**
 * AttributeSetEventValidator validates attribute set events.
 * 
 * @author Stephan Zuercher
 */
public class AttributeSetEventValidator extends AttributeEventValidator
{
    public AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        Object newValue,
        Object oldValue)
    {
        this(
            expectedEventType,
            attributeName,
            newValue, 
            oldValue,
            AttributeEvent.POSITION_NONE);
    }
    
    public AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        Object newValue,
        Object oldValue,
        int position)
    {
        super(expectedEventType, attributeName, newValue, oldValue, position);
    }
    
    public <E extends RefObject, F extends RefObject> AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        String valueAttributeName,
        Class<E> expectedNewType,
        Object expectedNewAttribValue,
        Class<F> expectedOldType,
        Object expectedOldAttribValue)
    {
        this(
            expectedEventType,
            attributeName,
            valueAttributeName,
            expectedNewType,
            expectedNewAttribValue,
            expectedOldType,
            expectedOldAttribValue,
            AttributeEvent.POSITION_NONE);
    }

    public <E extends RefObject, F extends RefObject> AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        String valueAttributeName,
        Class<E> expectedNewType,
        Object expectedNewAttribValue,
        Class<F> expectedOldType,
        Object expectedOldAttribValue,
        int position)
    {
        super(
            expectedEventType,
            attributeName,
            valueAttributeName,
            expectedNewType,
            expectedNewAttribValue,
            expectedOldType,
            expectedOldAttribValue,
            position);
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        Class<? extends RefObject> newType = null;
        if (expectedNewType != null) {
            newType = expectedNewType.asSubclass(RefObject.class);
        }
        
        Class<? extends RefObject> oldType = null;
        if (expectedOldType != null) {
            oldType = expectedOldType.asSubclass(RefObject.class);
        }
        
        return new AttributeSetEventValidator(
            newEventType,
            expectedAttributeName,
            valueAttributeName,
            newType,
            expectedNewValue,
            oldType,
            expectedOldValue,
            expectedPosition);            
    }
}

// End AttributeSetEventValidator.java
