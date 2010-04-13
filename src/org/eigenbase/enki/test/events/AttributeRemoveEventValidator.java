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
 * AttributeRemoveEventValidator validates attribute remove events for 
 * multi-valued attributes.
 * 
 * @author Stephan Zuercher
 */
public class AttributeRemoveEventValidator
    extends AttributeEventValidator
{
    public AttributeRemoveEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        Object expectedOldValue)
    {
        this(
            expectedEventType,
            expectedAttributeName,
            expectedOldValue,
            AttributeEvent.POSITION_NONE);
    }

    public AttributeRemoveEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        Object expectedOldValue,
        int position)
    {
        super(
            expectedEventType,
            expectedAttributeName,
            null, 
            expectedOldValue,
            position);
    }

    public <E extends RefObject> AttributeRemoveEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        String valueAttributeName,
        Class<E> expectedOldType,
        Object expectedOldValue)
    {
        this(
            expectedEventType,
            expectedAttributeName,
            valueAttributeName,
            expectedOldType,
            expectedOldValue,
            AttributeEvent.POSITION_NONE);
    }

    public <E extends RefObject> AttributeRemoveEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        String valueAttributeName,
        Class<E> expectedOldType,
        Object expectedOldValue,
        int position)
    {
        super(
            expectedEventType,
            expectedAttributeName,
            valueAttributeName,
            null,
            null,
            expectedOldType,
            expectedOldValue,
            position);
    }
    
    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        Class<? extends RefObject> oldType = null;
        if (expectedOldType != null) {
            oldType = expectedOldType.asSubclass(RefObject.class);
        }
        
        return new AttributeRemoveEventValidator(
            newEventType,
            expectedAttributeName,
            valueAttributeName,
            oldType,
            expectedOldValue,
            expectedPosition);
    }

}

// End AttributeRemoveEventValidator.java
