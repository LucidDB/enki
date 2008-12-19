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

import org.junit.*;
import org.netbeans.api.mdr.events.*;

/**
 * DuplicatEventValidator validates an event that is a duplicate, save for
 * event type, of another event.  Useful when validating the firing of a
 * planned event followed by the corresponding canceled or changed event.
 * 
 * @author Stephan Zuercher
 */
public class DuplicateEventValidator extends EventValidator
{
    /** Sequence number of the original. */
    private final int seq;
    
    /**
     * Constructs a new DuplicateEventValidator instance.
     * 
     * @param expectedEventType expected event type (typically differs from
     *                          the original event's type)
     * @param seq the sequence number (0-based) of the original event
     */
    public DuplicateEventValidator(EventType expectedEventType, int seq)
    {
        super(expectedEventType);
        
        this.seq = seq;
    }
    
    public void visitEvent(
        MDRChangeEvent event, EventType eventType, int unused)
    {
        Assert.fail(toString());
    }
    
    public String toString()
    {
        return 
            "Duplicate Seq#" + seq + 
            "(" + parent.getValidator(seq).toString() + ")";
    }

    public EventValidator makeClone(DelegatingEventValidator delegator)
    {
        return delegator.getValidator(seq).cloneWithNewExpectedEventType(
            expectedEventType);
    }
    
    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        throw new UnsupportedOperationException();
    }    
}

// End DuplicateEventValidator.java
