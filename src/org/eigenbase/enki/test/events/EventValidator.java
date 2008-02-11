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
 * EventValidator is a base class for validator of MDR Event API events.
 * 
 * @author Stephan Zuercher
 */
public abstract class EventValidator
{
    /** Expected event type. */
    protected final EventType expectedEventType;
    
    /** Owning DelegatingEventValidator, if any. */
    protected DelegatingEventValidator parent;
    
    /** First exception thrown. */
    protected Throwable thrown = null;

    
    public EventValidator(EventType expectedEventType)
    {
        this.expectedEventType = expectedEventType;
        this.parent = null;
    }
    
    public int getNumExpectedEvents()
    {
        return 1;
    }
    
    /**
     * Re-throws the first exception encountered by this EventValidator.
     * If the exception is an {@link Error} or {@link RuntimeException} it
     * is thrown directly.  If the exception is a check {@link Exception} or
     * other {@link Throwable} it is first wrapped in a 
     * {@link RuntimeException}.
     */
    public void rethrow()
    {
        if (thrown == null) {
            return;
        }
        
        if (thrown instanceof RuntimeException) {
            throw (RuntimeException)thrown;
        } else if (thrown instanceof Error) {
            throw (Error)thrown;
        } else {
            throw new RuntimeException(thrown);
        }
    }
    
    /**
     * Visit the given event.  Checks the event's type and then invokes
     * the appropriate <code>validateEvent</code> method for the given
     * event type.  Catches any exception thrown from the validator methods
     * and records the first one in {@link #thrown}.
     * 
     * @param event event to visit
     * @param eventType event type (planned, canceled, change)
     * @param seq sequence number of this event (0-based)
     */
    public void visitEvent(
        MDRChangeEvent event, EventType eventType, int seq)
    {
        try {
            Assert.assertEquals(
                getClass().getName() + "/seq#" + seq, 
                expectedEventType, eventType);

            visitEventIgnoringType(event, seq);
        } 
        catch(Throwable t) {
            if (thrown == null) {
                thrown = t;
            }
        }
    }
    
    /**
     * Invoke the validator method appropriate for the given event type.
     * 
     * @param event MDRChangeEvent
     * @param seq sequence number (0-based)
     */
    protected final void visitEventIgnoringType(
        MDRChangeEvent event, int seq)
    {
        if (event instanceof ExtentEvent) {
            validateEvent((ExtentEvent)event, seq);
        } else if (event instanceof TransactionEvent) {
            validateEvent((TransactionEvent)event, seq);
        } else if (event instanceof InstanceEvent) {
            validateEvent((InstanceEvent)event, seq);
        } else if (event instanceof AssociationEvent) {
            validateEvent((AssociationEvent)event, seq);
        } else if (event instanceof AttributeEvent) {
            validateEvent((AttributeEvent)event, seq);
        } else {
            Assert.fail("unknown event type: " + event.getClass());
        }
    }
    
    public void validateEvent(ExtentEvent event, int seq)
    {
        Assert.fail("Unexpected ExtentEvent at seq#" + seq);
    }

    public void validateEvent(TransactionEvent event, int seq)
    {
        Assert.fail("Unexpected TransactionEvent at seq#" + seq);
    }

    public void validateEvent(InstanceEvent event, int seq)
    {
        Assert.fail("Unexpected InstanceEvent at seq#" + seq);
    }

    public void validateEvent(AssociationEvent event, int seq)
    {
        Assert.fail("Unexpected AssociationEvent at seq#" + seq);
    }
    
    public void validateEvent(AttributeEvent event, int seq)
    {
        Assert.fail("Unexpected AttributeEvent at seq#" + seq);
    }
    
    public String toString()
    {
        return getClass().getSimpleName();
    }

    void setDelegatingEventValidator(DelegatingEventValidator parent)
    {
        this.parent = parent;
    }
    
    abstract EventValidator cloneWithNewExpectedEventType(
        EventType expectedEventType);
}

// End EventValidator.java
