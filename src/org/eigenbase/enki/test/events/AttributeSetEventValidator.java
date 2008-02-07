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
    private final Object oldValue;
    private final Object newValue;
    private final boolean valuesAreSeqNumbers; 
    private final int position;
    
    public AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        Object oldValue,
        Object newValue,
        boolean valuesAreSeqNumbers)
    {
        this(
            expectedEventType,
            attributeName, 
            oldValue,
            newValue, 
            valuesAreSeqNumbers,
            AttributeEvent.POSITION_NONE);
    }
    
    public AttributeSetEventValidator(
        EventType expectedEventType,
        String attributeName,
        Object oldValue,
        Object newValue,
        boolean valuesAreSeqNumbers,
        int position)
    {
        super(expectedEventType);

        this.attributeName = attributeName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.valuesAreSeqNumbers = valuesAreSeqNumbers;
        this.position = position;
    }

    @Override
    public void validateEvent(AttributeEvent event, int seq)
    {
        Assert.assertEquals(attributeName, event.getAttributeName());
        
        Object oldElem = event.getOldElement();
        Object newElem = event.getNewElement();
        if (valuesAreSeqNumbers) {
            Assert.assertTrue(
                toString(), oldValue == null || oldValue instanceof Integer);
            Assert.assertTrue(
                toString(), newValue == null || newValue instanceof Integer);
            
            int oldSeq = oldValue != null ? ((Integer)oldValue).intValue() : -1;
            int newSeq = newValue != null ? ((Integer)newValue).intValue() : -1;
            
            CreateInstanceEventValidator oldEvent = 
                oldValue != null 
                    ? (CreateInstanceEventValidator)parent.getValidator(oldSeq) : null;
            CreateInstanceEventValidator newEvent =
                newValue != null
                    ? (CreateInstanceEventValidator)parent.getValidator(newSeq) : null;
            
            RefObject oldRefValue = 
                oldValue != null ? oldEvent.getInstance() : null;
            RefObject newRefValue = 
                newValue != null ? newEvent.getInstance() : null;
            
            Assert.assertEquals(toString(), oldRefValue, oldElem);
            Assert.assertEquals(toString(), newRefValue, newElem);
        } else {
            Assert.assertFalse(oldElem instanceof RefObject);
            Assert.assertFalse(newElem instanceof RefObject);

            Assert.assertEquals(toString(), oldValue, oldElem);
            Assert.assertEquals(toString(), newValue, newElem);
        }
        
        Assert.assertEquals(toString(), position, event.getPosition());
    }

    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b.append(super.toString());
        b.append("(attribName=").append(attributeName);
        if (valuesAreSeqNumbers) {
            if (newValue != null) {
                b.append(", new=seq#").append(newValue);
            }
            if (oldValue != null) {
                b.append(", old=seq#").append(oldValue);
            }
        } else {
            if (newValue != null) {
                b.append(", new=").append(newValue);
            }
            if (oldValue != null) {
                b.append(", old=").append(oldValue);
            }
        }
        b.append(')');
        
        return b.toString();
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType expectedEventType)
    {
        return new AttributeSetEventValidator(
            expectedEventType,
            attributeName,
            oldValue,
            newValue,
            valuesAreSeqNumbers,
            position);
    }
}

// End AttributeSetEventValidator.java
