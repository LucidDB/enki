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
 * AssociationEventValidator is an abstract base class for event validators
 * related to {@link AssociationEvent} events.
 * 
 * @author Stephan Zuercher
 */
abstract class AssociationEventValidator extends EventValidator
{
    /** Expected fixed end type. */
    protected final Class<? extends RefObject> expectedFixedType;
    
    /** Expected end name. */
    protected final String expectedEndName;
    
    /** Name of an attribute of the expected fixed end.  E.g, "model". */
    protected final String fixedTypeAttrib;
    
    /** Expected value of the fixed end attribute. */
    protected final String expectedFixedAttribValue;

    /** Expected new end type (may be null). */
    protected final Class<? extends RefObject> expectedNewType;
    
    /** Name of an attribute of the new end.  E.g., "model."  May be null. */
    protected final String newTypeAttrib;
    
    /** Expected value of the new end attribute, if any. */
    protected final String expectedNewAttribValue;

    /** Expected old end type (may be null). */
    protected final Class<? extends RefObject> expectedOldType;

    /** Name of an attribute of the old end.  E.g., "model."  May be null. */
    protected final String oldTypeAttrib;

    /** Expected old end type (may be null). */
    protected final String expectedOldAttribValue;
    
    /** Expected position value. */
    protected final int expectedPosition;
    
    protected AssociationEventValidator(
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
        this(
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
            AssociationEvent.POSITION_NONE);
    }
    
    protected AssociationEventValidator(
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
        super(expectedEventType);
        
        this.expectedEndName = expectedEndName;
        this.expectedFixedType = expectedFixedType;
        this.fixedTypeAttrib = fixedTypeAttrib;
        this.expectedFixedAttribValue = expectedFixedAttribValue;
        
        this.expectedNewType = expectedNewType;
        this.newTypeAttrib = newTypeAttrib;
        this.expectedNewAttribValue = expectedNewAttribValue;
        
        this.expectedOldType = expectedOldType;
        this.oldTypeAttrib = oldTypeAttrib;
        this.expectedOldAttribValue = expectedOldAttribValue;
        
        this.expectedPosition = expectedPosition;
    }

    /**
     * Helper method to check the end name and position.
     *
     * @param event event to check
     */
    protected void checkEndNameAndPosition(AssociationEvent event)
    {
        Assert.assertEquals(toString(), expectedEndName, event.getEndName());
        
        Assert.assertEquals(toString(), expectedPosition, event.getPosition());
    }
    
    /**
     * Helper method to check the type and an attribute of the fixed, new
     * or old end.
     *
     * @param refObject entity to check
     * @param expectedType entity's expected type 
     *                     (e.g. {@link #expectedFixedType},
     *                     {@link #expectedNewType}, or 
     *                     {@link #expectedOldType})
     * @param attribName attribute name (may be null to prevent test)
     * @param expectedAttribValue expected attribute value
     */
    protected void check(
        RefObject refObject, 
        Class<? extends RefObject> expectedType,
        String attribName,
        String expectedAttribValue)
    {
        Assert.assertTrue(toString(), expectedType.isInstance(refObject));
        if (attribName != null) {
            Assert.assertEquals(
                toString(), 
                expectedAttribValue, 
                refObject.refGetValue(attribName).toString());
        }
    }

    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append(super.toString());
        b.append("(endName=").append(expectedEndName); 
        b
            .append(", fixed=")
            .append(expectedFixedType)
            .append('/')
            .append(fixedTypeAttrib)
            .append('/')
            .append(expectedFixedAttribValue);
        if (expectedNewType != null) {
            b
                .append(", new=")
                .append(expectedNewType)
                .append('/')
                .append(newTypeAttrib)
                .append('/')
                .append(expectedNewAttribValue);
        }
        if (expectedOldType != null) {
            b
            .append(", old=")
            .append(expectedOldType)
            .append('/')
            .append(oldTypeAttrib)
            .append('/')
            .append(expectedOldAttribValue);                
        }

        return b.toString();
    }
}

// End AssociationEventValidator.java
