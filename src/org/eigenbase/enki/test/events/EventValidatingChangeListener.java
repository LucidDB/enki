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
 * EventValidatingChangeListener is an MDR event API listener that validates events according
 * to the given {@link EventValidator} as they arrive.
 * 
 * @author Stephan Zuercher
 */
public class EventValidatingChangeListener implements MDRPreChangeListener
{
    private final EventValidator eventValidator;
    private final boolean printEvents;
    private int seq;
    
    /**
     * Constructs a new EventValidatingChangeListener.
     * 
     * @param eventValidator used for event validation
     * @param printEvents if true, print event details as they arrive
     */
    public EventValidatingChangeListener(
        EventValidator eventValidator, boolean printEvents)
    {
        this.eventValidator = eventValidator;
        this.printEvents = printEvents;
        this.seq = 0;
    }
    
    public int getNumEventsExpected()
    {
        return eventValidator.getNumExpectedEvents();
    }
    
    /**
     * Re-throws the first exception caught by the event validator.
     */
    public void rethrow()
    {
        eventValidator.rethrow();
    }
    
    // Implement MDRPreChangeListner
    public synchronized void change(MDRChangeEvent event)
    {
        logEvent(EventType.CHANGED, event);
        eventValidator.visitEvent(event, EventType.CHANGED, seq++);
        notifyAll();
    }

    // Implement MDRPreChangeListner
    public synchronized void plannedChange(MDRChangeEvent event)
    {
        logEvent(EventType.PLANNED, event);
        eventValidator.visitEvent(event, EventType.PLANNED, seq++);
        notifyAll();
    }

    // Implement MDRPreChangeListner
    public synchronized void changeCancelled(MDRChangeEvent event)
    {
        logEvent(EventType.CANCELED, event);
        eventValidator.visitEvent(event, EventType.CANCELED, seq++);
        notifyAll();
    }

    /**
     * Dump events to standard output if printing is enabled.
     * 
     * @param eventType event type
     * @param event event to print
     */
    private void logEvent(EventType eventType, MDRChangeEvent event)
    {
        if (!printEvents) {
            return;
        }
        
        StringBuilder b = new StringBuilder();
        
        b
            .append("Event: ")
            .append(eventType)
            .append(": " )
            .append(event.getClass().getSimpleName())
            .append('/')
            .append(System.identityHashCode(event))
            .append(": ");

        if (event instanceof ExtentEvent) {
            ExtentEvent ee = (ExtentEvent)event;
            b.append("Extent: ").append(ee.getExtentName()).append(", ");
            switch(ee.getType()) {
            case ExtentEvent.EVENT_EXTENT_CREATE:
                b.append("create");
                break;
                
            case ExtentEvent.EVENT_EXTENT_DELETE:
                b.append("delete");
                break;
                
            default:
                b.append("???");
                break;
            }
        } else if (event instanceof TransactionEvent) {
            TransactionEvent te = (TransactionEvent)event;
            switch(te.getType()) {
            case TransactionEvent.EVENT_TRANSACTION_START:
                b.append("txn start");
                break;
            case TransactionEvent.EVENT_TRANSACTION_END:
                b.append("txn end");
                break;
                
            default:
                b.append("???");
                break;
            }
        } else if (event instanceof InstanceEvent) {
            InstanceEvent ie = (InstanceEvent)event;
            switch(ie.getType()) {
            case InstanceEvent.EVENT_INSTANCE_CREATE:
                b.append("create ");
                break;
                
            case InstanceEvent.EVENT_INSTANCE_DELETE:
                b.append("delete ");
                break;
                
            default:
                b.append("??? ");
                break;
            }
            
            b.append(describe((RefFeatured)ie.getSource()));
            if (ie.getArguments() != null) {
                b.append(ie.getArguments().toString());
            } else {
                b.append("[]");
            }
        } else if (event instanceof AssociationEvent) {
            AssociationEvent ae = (AssociationEvent)event;
            b
                .append(describe(ae.getFixedElement()))
                .append('(')
                .append(ae.getEndName())
                .append(')');
            switch(ae.getType()) {
            case AssociationEvent.EVENT_ASSOCIATION_ADD:
                b
                    .append(" add ")
                    .append(describe(ae.getNewElement()));
                break;
                
            case AssociationEvent.EVENT_ASSOCIATION_REMOVE:
                b
                    .append(" remove ")
                    .append(describe(ae.getOldElement()));
                break;

            case AssociationEvent.EVENT_ASSOCIATION_SET:
                b
                    .append(" set ")
                    .append(describe(ae.getNewElement()))
                    .append(" replacing ")
                    .append(describe(ae.getOldElement()));
                break;
                
            default:
                b
                    .append(" ??? new ")
                    .append(describe(ae.getNewElement()))
                    .append(" old ")
                    .append(describe(ae.getOldElement()));
                break;
            }

            if (ae.getPosition() == AssociationEvent.POSITION_NONE) {
                b.append(" (no position)");
            } else {
                b.append(" (at ").append(ae.getPosition()).append(")");
            }
        } else if (event instanceof AttributeEvent) {
            AttributeEvent ae = (AttributeEvent) event;
            
            b.append(ae.getAttributeName());

            switch(ae.getType()) {
            case AttributeEvent.EVENT_ATTRIBUTE_ADD:
                b
                    .append(" add ")
                    .append(describe(ae.getNewElement()));
                break;
                
            case AttributeEvent.EVENT_ATTRIBUTE_REMOVE:
                b
                    .append(" remove ")
                    .append(describe(ae.getOldElement()));
                break;

            case AttributeEvent.EVENT_ATTRIBUTE_SET:
                b
                    .append(" set ")
                    .append(describe(ae.getNewElement()))
                    .append(" replacing ")
                    .append(describe(ae.getOldElement()));
                break;

            case AttributeEvent.EVENT_CLASSATTR_ADD:
            case AttributeEvent.EVENT_CLASSATTR_REMOVE:
            case AttributeEvent.EVENT_CLASSATTR_SET:
                assert(false);
            default:
                b
                    .append(" ??? new ")
                    .append(describe(ae.getNewElement()))
                    .append(" old ")
                    .append(describe(ae.getOldElement()));
                break;

            }
            

            if (ae.getPosition() == AttributeEvent.POSITION_NONE) {
                b.append(" (no position)");
            } else {
                b.append(" (at ").append(ae.getPosition()).append(")");
            }
        } else {
            b.append("unknown event type: " + event.getClass().getName());
        }
        
        b
            .append("  (Thread: ")
            .append(Thread.currentThread().getName())
            .append(")");
        
        System.out.println(b.toString());
    }

    private String describe(Object obj)
    {
        if (obj instanceof RefFeatured) {
            return describe((RefFeatured)obj);
        }
        
        return String.valueOf(obj);
    }
    
    private String describe(RefFeatured obj)
    {
        if (obj instanceof RefClass) {
            return String.valueOf(obj.refMetaObject().refGetValue("name"));
        }
        
        assert(obj instanceof RefObject);
        
        RefObject o = (RefObject)obj;
        StringBuilder b = new StringBuilder();

        RefClass cls = null;
        try {
            cls = o.refClass();
        }
        catch(InvalidObjectException e) {
            // Likely that o has been deleted.  Ignore this.
        }

        if (cls != null) {
            b.append(describe(cls));
        } else {
            b.append(o.getClass().getSimpleName());
        }

        b
            .append('/')
            .append(o.refMofId());
        
        return b.toString();
    }
    
    /**
     * Blocks until the listener has received at least one event.  Equivalent
     * to {@link #waitForEvent(int) waitForEvent(1)}.
     */
    public void waitForEvent()
    {
        waitForSomeEvents(1);
    }
    
    /**
     * Blocks until the listener has received at least one event or the given
     * number of milliseconds passes.  Equivalent
     * to {@link #waitForEvent(int, long) waitForEvent(1, timeout)}.
     * 
     * @param timeoutMillis timeout period in milliseconds
     * @return true if the timeout elapsed before the event arrived
     */
    public boolean waitForEvent(long timeoutMillis) 
    {
        return waitForSomeEvents(1, timeoutMillis);
    }
    
    /**
     * Blocks until the listener has received at least the given number of
     * events.  Equivalent to 
     * {@link #waitForEvent(int, long) waitForEvent(numEvents, -1L)}.
     * 
     * @param numEvents number of events to wait for
     */
    public void waitForSomeEvents(int numEvents) 
    {
        waitForSomeEvents(numEvents, -1L);
    }
    
    
    /**
     * Blocks until the listener has received at least the given number of
     * events or the given number of milliseconds passes,
     * 
     * @param numEvents number of events to wait for
     * @param timeoutMillis timeout period in milliseconds
     * @return true if the timeout elapsed before the event arrived
     */
    public boolean waitForSomeEvents(int numEvents, long timeoutMillis)
    {
        long start = System.currentTimeMillis();
        try {
            long timeout = timeoutMillis;
            synchronized(this) {
                boolean first = true;
                while(true) {
                    if (seq >= numEvents) {
                        return false;
                    } else if (!first) {
                        long elapsed = System.currentTimeMillis() - start;
                        
                        if (elapsed >= timeoutMillis) {
                            return true;
                        }
                        
                        timeout = timeoutMillis - elapsed;
                    } else {
                        first = false;
                    }
                    
                    wait(timeout);
                }
            }
        } catch(InterruptedException e) {
            Assert.fail("Interrupted");
            return false; // unreachable
        } finally {
            long end = System.currentTimeMillis();
            if (printEvents) {
                System.out.println("slept " + (end - start) + " millis");
            }
        }
    }
}

// End EventValidatingChangeListener.java
