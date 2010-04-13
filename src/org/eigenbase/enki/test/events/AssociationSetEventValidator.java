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
 * AssociationSetEventValidator validates events related to modified 
 * associations.
 * 
 * @author Stephan Zuercher
 */
public class AssociationSetEventValidator extends AssociationEventValidator
{
    public AssociationSetEventValidator(
        EventType expectedEventType,
        String expectedEndName,
        Class<? extends RefObject> expectedFixedType,
        String fixedTypeAttrib,
        String expectedFixedAttribValue,
        Class<? extends RefObject> expectedNewType,
        String newTypeAttrib,
        String expectedNewAttribValue,
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
            expectedNewType,
            newTypeAttrib,
            expectedNewAttribValue,
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue);
    }

    public AssociationSetEventValidator(
        EventType expectedEventType,
        String expectedEndName,
        Class<? extends RefObject> expectedFixedType,
        String fixedTypeAttrib,
        String expectedFixedAttribValue,
        Class<? extends RefObject> expectedNewType,
        String newTypeAttrib,
        String expectedNewAttribValue,
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
            expectedNewType,
            newTypeAttrib,
            expectedNewAttribValue,
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue,
            expectedPosition);
    }

    @Override
    public void validateEvent(AssociationEvent event, int seq)
    {
        Assert.assertTrue(
            event.isOfType(AssociationEvent.EVENT_ASSOCIATION_SET));
        
        checkEndNameAndPosition(event);
        
        check(
            event.getFixedElement(), 
            expectedFixedType, 
            fixedTypeAttrib, 
            expectedFixedAttribValue);
        check(
            event.getNewElement(),
            expectedNewType,
            newTypeAttrib,
            expectedNewAttribValue);
        check(
            event.getOldElement(),
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue);
    }
    
    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        return new AssociationSetEventValidator(
            newEventType,
            expectedEndName,
            expectedFixedType,
            fixedTypeAttrib,
            expectedFixedAttribValue,
            expectedNewType,
            newTypeAttrib,
            expectedNewAttribValue,
            expectedOldType,
            oldTypeAttrib,
            expectedOldAttribValue,
            expectedPosition);
    }
}

// End AssociationSetEventValidator.java
