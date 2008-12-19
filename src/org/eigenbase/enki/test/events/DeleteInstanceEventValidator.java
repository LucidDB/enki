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
 * DeleteInstanceEventValidator validates instance events related to deletion.
 * 
 * @author Stephan Zuercher
 */
public class DeleteInstanceEventValidator
    extends EventValidator
{
    private final String mofId;
    
    public DeleteInstanceEventValidator(
        EventType expectedEventType, String mofId)
    {
        super(expectedEventType);
        
        this.mofId = mofId;
    }
    
    @Override
    public void validateEvent(InstanceEvent event, int seq)
    {
        Assert.assertTrue(
            toString(),
            event.isOfType(InstanceEvent.EVENT_INSTANCE_DELETE));
        
        RefObject instance = event.getInstance();
        
        Assert.assertEquals(toString(), mofId, instance.refMofId());
    }
    
    @Override
    public String toString()
    {
        StringBuilder b = new StringBuilder();
        b
            .append(super.toString())
            .append("(instance=")
            .append(mofId)
            .append(')');
        
        return b.toString();
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        return new DeleteInstanceEventValidator(newEventType, mofId);
    }

}

// End DeleteInstanceEventValidator.java
