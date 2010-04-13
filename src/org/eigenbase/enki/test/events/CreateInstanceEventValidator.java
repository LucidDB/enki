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

import java.util.*;

import javax.jmi.reflect.*;

import org.junit.*;
import org.netbeans.api.mdr.events.*;

/**
 * CreateInstanceEventValidator validates instance creation events.
 * 
 * @author Stephan Zuercher
 */
public class CreateInstanceEventValidator extends EventValidator
{
    /** Expected event source. */
    private final RefClass expectedSource;
    
    /** Expected instance type (changed/canceled event only). */
    private final Class<? extends RefObject> expectedInstanceType;
    /** 
     * Expected arguments to instance creation.  Set to null for no-argument 
     * constructor.
     */
    private final List<Object> expectedArgs;

    private InstanceEvent event;
    
    public CreateInstanceEventValidator(
        EventType expectedEventType,
        RefClass expectedSource,
        Class<? extends RefObject> expectedInstanceType,
        List<Object> expectedArgs)
    {
        super(expectedEventType);
        this.expectedSource = expectedSource;
        this.expectedInstanceType = expectedInstanceType;
        this.expectedArgs = expectedArgs;
        this.event = null;
    }

    @Override
    public void validateEvent(InstanceEvent instanceEvent, int seq)
    {
        this.event = instanceEvent;
        
        Assert.assertTrue(
            toString(),
            instanceEvent.isOfType(
                InstanceEvent.EVENT_INSTANCE_CREATE));

        Assert.assertEquals(
            toString(), expectedSource, instanceEvent.getSource());
        
        RefObject instance = instanceEvent.getInstance();
        if (expectedEventType == EventType.PLANNED) {
            Assert.assertNull(toString(), instance);
        } else {
            Assert.assertNotNull(toString(), instance);
            Assert.assertTrue(
                toString(), 
                expectedInstanceType.isInstance(instance));
        }
        
        Assert.assertEquals(
            toString(), expectedArgs, instanceEvent.getArguments());
    }
    
    public RefObject getInstance()
    {
        if (event != null) {
            return event.getInstance();
        }
        
        return null;
    }
    
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b
            .append(super.toString())
            .append("(sourceType=")
            .append(expectedSource.refMetaObject().refGetValue("name"))
            .append(", expectedInstanceType=")
            .append(expectedInstanceType.getSimpleName())
            .append(", expectedArgs=")
            .append(expectedArgs)
            .append(")");
        return b.toString();
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        return new CreateInstanceEventValidator(
            newEventType,
            expectedSource,
            expectedInstanceType,
            expectedArgs);
    }
}

// End CreateInstanceEventValidator.java
