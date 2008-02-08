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
 * AttributeSetEventValidator validates attribute set events.
 * 
 * @author Stephan Zuercher
 */
public class AttributeSetEventValidator extends EventValidator
{
    private final String attributeName;

    private final boolean valuesAreSimpleObjects;
    private final Object newValue;
    private final Object oldValue;

    private final String valueAttributeName;
    private final Class<? extends RefObject> expectedNewType;
    private final Class<? extends RefObject> expectedOldType;
    
    private final int position;
    
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
        super(expectedEventType);

        this.attributeName = attributeName;
        this.valuesAreSimpleObjects = true;
        this.newValue = newValue;
        this.oldValue = oldValue;
        this.position = position;

        this.valueAttributeName = null;
        this.expectedNewType = null;
        this.expectedOldType = null;
    }
    
    public AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        String valueAttributeName,
        Class<? extends RefObject> expectedNewType,
        Object expectedNewAttribValue,
        Class<? extends RefObject> expectedOldType,
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

    public AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        String valueAttributeName,
        Class<? extends RefObject> expectedNewType,
        Object expectedNewAttribValue,
        Class<? extends RefObject> expectedOldType,
        Object expectedOldAttribValue,
        int position)
    {
        super(expectedEventType);
        
        this.attributeName = attributeName;
        this.valuesAreSimpleObjects = false;
        this.valueAttributeName = valueAttributeName;
        this.expectedNewType = expectedNewType;
        this.newValue = expectedNewAttribValue;
        this.expectedOldType = expectedOldType;
        this.oldValue = expectedOldAttribValue;
        this.position = position;
    }

    @Override
    public void validateEvent(AttributeEvent event, int seq)
    {
        Assert.assertEquals(attributeName, event.getAttributeName());
        
        Object newElem = event.getNewElement();
        Object oldElem = event.getOldElement();
        
        if (valuesAreSimpleObjects) {
            Assert.assertFalse(newElem instanceof RefObject);
            Assert.assertFalse(oldElem instanceof RefObject);

            Assert.assertEquals(toString(), newValue, newElem);
            Assert.assertEquals(toString(), oldValue, oldElem);
        } else {
            check(expectedNewType, newValue, newElem);
            check(expectedOldType, oldValue, oldElem);
        }
        
        Assert.assertEquals(toString(), position, event.getPosition());
    }

    private void check(
        Class<? extends RefObject> expectedType,
        Object expectedAttribValue,
        Object givenValue)
    {
        if (expectedType == null) {
            Assert.assertNull(toString(), givenValue);
            return;
        }
        
        Assert.assertTrue(toString(), expectedType.isInstance(givenValue));
        
        RefObject refGivenValue = expectedType.cast(givenValue);
        
        Object givenAttribValue = 
            refGivenValue.refGetValue(valueAttributeName);
        
        Assert.assertEquals(toString(), expectedAttribValue, givenAttribValue);
    }
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append(super.toString());
        b.append("(attribName=").append(attributeName);
        if (valuesAreSimpleObjects) {
          b
              .append(", new=")
              .append(newValue)
              .append(", old=")
              .append(oldValue);
        } else {
            b.append(", new=");
            if (expectedNewType != null) {
                b
                    .append(expectedNewType.getSimpleName())
                    .append('/')
                    .append(valueAttributeName)
                    .append('/')
                    .append(newValue);
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
                    .append(oldValue);                
            } else {
                b.append("null");
            }
        }
        b.append(')');
        
        return b.toString();
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType expectedEventType)
    {
        if (valuesAreSimpleObjects) {
            return new AttributeSetEventValidator(
                expectedEventType,
                attributeName,
                newValue,
                oldValue,
                position);
        } else {
            return new AttributeSetEventValidator(
                expectedEventType,
                attributeName,
                valueAttributeName,
                expectedNewType,
                newValue,
                expectedOldType,
                oldValue,
                position);
                
        }
    }
}

// End AttributeSetEventValidator.java
