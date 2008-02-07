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

import org.junit.*;
import org.netbeans.api.mdr.events.*;

/**
 * AssociationAddEventValidator validates events related to new associations.
 * 
 * @author Stephan Zuercher
 */
public class AssociationAddEventValidator extends AssociationEventValidator
{
    public AssociationAddEventValidator(
        EventType expectedEventType,
        String expectedEndName,
        Class<? extends RefObject> expectedFixedType,
        String fixedTypeAttrib,
        String expectedFixedAttribValue,
        Class<? extends RefObject> expectedNewType,
        String newTypeAttrib,
        String expectedNewAttribValue)
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
            null,
            null,
            null);
    }

    @Override
    public void validateEvent(AssociationEvent event, int seq)
    {
        Assert.assertTrue(
            event.isOfType(AssociationEvent.EVENT_ASSOCIATION_ADD));
        
        checkEndName(event);
        
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
        Assert.assertNull(event.getOldElement());
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType expectedEventType)
    {
        return new AssociationAddEventValidator(
            expectedEventType,
            expectedEndName,
            expectedFixedType,
            fixedTypeAttrib,
            expectedFixedAttribValue,
            expectedNewType,
            newTypeAttrib,
            expectedNewAttribValue);
    }
}

// End AssociationAddEventValidator.java
