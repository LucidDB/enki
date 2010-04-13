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

import org.junit.*;
import org.netbeans.api.mdr.events.*;

/**
 * AssociationRemoveEventValidator validates events related to removed
 * associations.
 * 
 * @author Stephan Zuercher
 */
public class AssociationRemoveEventValidator 
    extends AssociationEventValidator
{
    public AssociationRemoveEventValidator(
        EventType expectedEventType,
        String expectedEndName,
        Class<? extends RefObject> expectedFixedType,
        String fixedTypeAttrib,
        String expectedFixedAttribValue,
        Class<? extends RefObject> expectedOldType,
        String oldTypeAttrib,
        String expectedOldAttribValue)
    {
        super(
            expectedEventType,
            expectedEndName,
            expectedFixedType,
            fixedTypeAttrib,
            expectedFixedAttribValue,
            null,
            null,
            null,
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue);
    }

    public AssociationRemoveEventValidator(
        EventType expectedEventType,
        String expectedEndName,
        Class<? extends RefObject> expectedFixedType,
        String fixedTypeAttrib,
        String expectedFixedAttribValue,
        Class<? extends RefObject> expectedOldType,
        String oldTypeAttrib,
        String expectedOldAttribValue,
        int expectedPosition)
    {
        super(
            expectedEventType,
            expectedEndName,
            expectedFixedType,
            fixedTypeAttrib,
            expectedFixedAttribValue,
            null,
            null,
            null,
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue,
            expectedPosition);
    }

    @Override
    public void validateEvent(AssociationEvent event, int seq)
    {
        Assert.assertTrue(
            event.isOfType(AssociationEvent.EVENT_ASSOCIATION_REMOVE));
        
        checkEndNameAndPosition(event);
        
        check(
            event.getFixedElement(), 
            expectedFixedType, 
            fixedTypeAttrib, 
            expectedFixedAttribValue);
        check(
            event.getOldElement(),
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue);
        Assert.assertNull(event.getNewElement());
    }
    
    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        return new AssociationRemoveEventValidator(
            newEventType,
            expectedEndName,
            expectedFixedType,
            fixedTypeAttrib,
            expectedFixedAttribValue,
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue,
            expectedPosition);
    }
}

// End AssociationRemoveEventValidator.java
