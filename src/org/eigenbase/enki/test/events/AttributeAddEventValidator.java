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
package org.eigenbase.enki.test.events;

import javax.jmi.reflect.*;

import org.netbeans.api.mdr.events.*;

/**
 * AttributeAddEventValidator validates attribute add events for multi-valued
 * attributes.
 * 
 * @author Stephan Zuercher
 */
public class AttributeAddEventValidator
    extends AttributeEventValidator
{
    public AttributeAddEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        Object expectedNewValue)
    {
        this(
            expectedEventType,
            expectedAttributeName,
            expectedNewValue,
            AttributeEvent.POSITION_NONE);
    }

    public AttributeAddEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        Object expectedNewValue,
        int position)
    {
        super(
            expectedEventType,
            expectedAttributeName,
            expectedNewValue,
            null, 
            position);
    }

    public <E extends RefObject> AttributeAddEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        String valueAttributeName,
        Class<E> expectedNewType,
        Object expectedNewValue)
    {
        this(
            expectedEventType,
            expectedAttributeName,
            valueAttributeName,
            expectedNewType,
            expectedNewValue,
            AttributeEvent.POSITION_NONE);
    }

    public <E extends RefObject> AttributeAddEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        String valueAttributeName,
        Class<E> expectedNewType,
        Object expectedNewValue,
        int position)
    {
        super(
            expectedEventType,
            expectedAttributeName,
            valueAttributeName,
            expectedNewType,
            expectedNewValue,
            null,
            null,
            position);
    }
    
    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        Class<? extends RefObject> newType = null;
        if (expectedNewType != null) {
            newType = expectedNewType.asSubclass(RefObject.class);
        }
        
        return new AttributeAddEventValidator(
            newEventType,
            expectedAttributeName,
            valueAttributeName,
            newType,
            expectedNewValue,
            expectedPosition);
    }

}

// End AttributeAddEventValidator.java
