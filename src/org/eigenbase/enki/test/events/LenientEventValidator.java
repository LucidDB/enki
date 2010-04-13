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

import org.netbeans.api.mdr.events.*;

/**
 * LenientEventValidator accepts all known event types without testing
 * any aspect of the event (including expected event type).  Its primary
 * purpose is to allow unit tests to guarantee that all events related to
 * test case set-up have been emitted before beginning the actual test.
 * 
 * @author Stephan Zuercher
 */
public class LenientEventValidator extends EventValidator
{
    private final int numExpectedEvents;
    
    public LenientEventValidator(int numExpectedEvents)
    {
        // Dummy expected event type.
        super(null);
        
        this.numExpectedEvents = numExpectedEvents;
    }

    @Override 
    public int getNumExpectedEvents()
    {
        return numExpectedEvents;
    }
    
    @Override
    public void validateEvent(AssociationEvent event, int seq)
    {
    }

    @Override
    public void validateEvent(AttributeEvent event, int seq)
    {
    }

    @Override
    public void validateEvent(ExtentEvent event, int seq)
    {
    }

    @Override
    public void validateEvent(InstanceEvent event, int seq)
    {
    }

    @Override
    public void validateEvent(TransactionEvent event, int seq)
    {
    }

    @Override
    public void visitEvent(
        MDRChangeEvent event,
        EventType eventType,
        int seq)
    {
        visitEventIgnoringType(event, seq);
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        return new LenientEventValidator(numExpectedEvents);
    }
}

// End LenientEventValidator.java
