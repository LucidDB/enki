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
 * AttributeEventValidator is an abstract base class for event validators
 * related to {@link AttributeEvent} events.
 * 
 * @author Stephan Zuercher
 */
abstract class AttributeEventValidator extends EventValidator
{
    /** Expected attribute name. */
    protected final String expectedAttributeName;
    
    /** Name of an attribute of the value.  E.g., "model."  May be null. */
    protected final String valueAttributeName;
    
    /** Expected new end type (may be null). */
    protected final Class<?> expectedNewType;
    
    /** Expected value of the new end attribute, if any. */
    protected final Object expectedNewValue;

    /** Expected old end type (may be null). */
    protected final Class<?> expectedOldType;

    /** Expected old end value (may be null). */
    protected final Object expectedOldValue;
    
    /** Expected position value. */
    protected final int expectedPosition;

    protected AttributeEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        Object expectedNewValue,
        Object expectedOldValue)
    {
        super(expectedEventType);
        
        this.expectedAttributeName = expectedAttributeName;
        this.expectedNewValue = expectedNewValue;
        this.expectedOldValue = expectedOldValue;
        
        this.valueAttributeName = null;
        this.expectedNewType = null;
        this.expectedOldType = null;
        this.expectedPosition = AttributeEvent.POSITION_NONE;
    }    

    protected AttributeEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        Object expectedNewValue,
        Object expectedOldValue,
        int expectedPosition)
    {
        super(expectedEventType);
        
        this.expectedAttributeName = expectedAttributeName;
        this.expectedNewValue = expectedNewValue;
        this.expectedOldValue = expectedOldValue;
        this.expectedPosition = expectedPosition;
        
        this.valueAttributeName = null;
        this.expectedNewType = null;
        this.expectedOldType = null;
    }    

    protected <E extends RefObject, F extends RefObject> AttributeEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        String valueAttributeName,
        Class<E> expectedNewType,
        Object expectedNewValue,
        Class<F> expectedOldType,
        Object expectedOldValue)
    {
        super(expectedEventType);
        
        this.expectedAttributeName = expectedAttributeName;
        this.valueAttributeName = valueAttributeName;
        this.expectedNewType = expectedNewType;
        this.expectedNewValue = expectedNewValue;
        this.expectedOldType = expectedOldType;
        this.expectedOldValue = expectedOldValue;
        this.expectedPosition = AttributeEvent.POSITION_NONE;
    }    

    protected <E extends RefObject, F extends RefObject> AttributeEventValidator(
        EventType expectedEventType,
        String expectedAttributeName,
        String valueAttributeName,
        Class<E> expectedNewType,
        Object expectedNewValue,
        Class<F> expectedOldType,
        Object expectedOldValue,
        int expectedPosition)
    {
        super(expectedEventType);
        
        this.expectedAttributeName = expectedAttributeName;
        this.expectedNewType = expectedNewType;
        this.valueAttributeName = valueAttributeName;
        this.expectedNewValue = expectedNewValue;
        this.expectedOldType = expectedOldType;
        this.expectedOldValue = expectedOldValue;
        this.expectedPosition = expectedPosition;
    }    
    
    @Override
    public void validateEvent(AttributeEvent event, int seq)
    {
        checkAttributeNameAndPosition(event);

        check(expectedNewType, expectedNewValue, event.getNewElement());
        check(expectedOldType, expectedOldValue, event.getOldElement());
    }
    
    protected void checkAttributeNameAndPosition(AttributeEvent event)
    {
        Assert.assertEquals(
            toString(), expectedAttributeName, event.getAttributeName());

        Assert.assertEquals(toString(), expectedPosition, event.getPosition());
    }
    
    protected void check(
        Class<?> expectedType, 
        Object expectedValue, 
        Object givenValue)
    {
        if (expectedType != null) {
            Assert.assertTrue(toString(), expectedType.isInstance(givenValue));

            RefObject refObject = (RefObject)givenValue;
            Object comparisonValue = refObject.refGetValue(valueAttributeName);
            Assert.assertEquals(toString(), expectedValue, comparisonValue);
        } else {
            Assert.assertEquals(toString(), expectedValue, givenValue);
        }
    }
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append(super.toString());
        b.append("(attribName=").append(expectedAttributeName);
        if (expectedNewType == null && expectedOldType == null) {
          b
              .append(", new=")
              .append(expectedNewValue)
              .append(", old=")
              .append(expectedOldValue);
        } else {
            b.append(", new=");
            if (expectedNewType != null) {
                b
                    .append(expectedNewType.getSimpleName())
                    .append('/')
                    .append(valueAttributeName)
                    .append('/')
                    .append(expectedNewValue);
            } else {
                b.append("null");
            }
            
            b.append(", old=");
            if (expectedOldType != null) {
                b
                    .append(expectedOldType.getSimpleName())
                    .append('/')
                    .append(valueAttributeName)
                    .append('/')
                    .append(expectedOldValue);                
            } else {
                b.append("null");
            }
        }
        b.append(')');
        
        return b.toString();
    }
}

// End AttributeEventValidator.java
