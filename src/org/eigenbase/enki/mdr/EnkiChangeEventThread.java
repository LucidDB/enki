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
package org.eigenbase.enki.mdr;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import org.netbeans.api.mdr.*;
import org.netbeans.api.mdr.events.*;

/**
 * EnkiChangeEventThread issues {@link MDRChangeEvent events} asynchronously
 * upon completion of a write transaction.
 *  
 * @author Stephan Zuercher
 */
public class EnkiChangeEventThread
    extends Thread
{
    private static final long POLLING_TIMEOUT = 5000L;
    private static final int MAX_CONSECUTIVE = 1000;
    private static final CheckShutdownEvent CHECK_SHUTDOWN_EVENT = 
        new CheckShutdownEvent();
    
    private static final Logger log = 
        Logger.getLogger(EnkiChangeEventThread.class.getName());
    
    private final ListenerSource listenerSource;
    private final LinkedBlockingQueue<MDRChangeEvent> eventQueue;
    
    private boolean shutdown;
    
    public EnkiChangeEventThread(ListenerSource listenerSource)
    {
        this.listenerSource = listenerSource;
        this.eventQueue = new LinkedBlockingQueue<MDRChangeEvent>();
        this.shutdown = false;
        
        setName("Enki MDR Change Event Thread");
        setDaemon(true);
    }
    
    public synchronized void shutdown() throws InterruptedException
    {
        // Protected against simultaneous shutdown calls.
        shutdown = true;
        eventQueue.clear();
        
        // If there are no events in the queue, place this one to cause the
        // thread to wake up sooner.
        eventQueue.offer(CHECK_SHUTDOWN_EVENT);
        join();
    }
    
    public void enqueueEvent(MDRChangeEvent event)
    {
        eventQueue.offer(event);
    }
    
    public void run()
    {
        MDRChangeEvent event;

        ArrayList<EnkiMaskedMDRChangeListener> listeners = 
            new ArrayList<EnkiMaskedMDRChangeListener>();
        
        while(true) {
            synchronized(this) {
                if (shutdown) {
                    break;
                }
            }
        
            try {
                int numConsecutive = 0;
                while(true) {
                    if (numConsecutive == 0) {
                        event = 
                           eventQueue.poll(
                               POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
                    } else {
                        event = eventQueue.poll();
                    }
                    
                    if (event == null || event == CHECK_SHUTDOWN_EVENT) {
                        // Check shutdown flag and go back to sleep.
                        break;
                    }
                    
                    listenerSource.getListeners(listeners);
                    
                    for(MDRChangeListener listener: listeners) { 
                        try {
                            listener.change(event);
                        }
                        catch(Throwable t) {
                            log.log(
                                Level.SEVERE,
                                "Unexpected exception in EnkiChangeEventThread",
                                t);
                        }
                    }

                    // Check the shutdown flag occasionally even when we're
                    // swamped.
                    if (++numConsecutive > MAX_CONSECUTIVE) {
                        break;
                    }
                }
            }
            catch(InterruptedException e) {
                log.log(Level.SEVERE, "EnkiChangeEventThread interrupted", e);
            }
        }
    }
    
    /**
     * ListenerSource represents an object that is a source of 
     * {@link MDRChangeListeners} instances.  Typically, this object is
     * also an {@link MDRepository}.
     */
    public interface ListenerSource
    {
        /**
         * Copy all current listeners into the given collection, clearing any
         * existing elements first.
         * 
         * @param listeners collection that will contain all current listeners
         */
        public void getListeners(
            Collection<EnkiMaskedMDRChangeListener> listeners);
    }
    
    /**
     * CheckShutdownEvent is a fake event used to speed up thread shutdown.
     */
    private static class CheckShutdownEvent extends MDRChangeEvent
    {
        private static final long serialVersionUID = 1L;

        public CheckShutdownEvent()
        {
            super(EnkiChangeEventThread.class, 0);
        }
    }
}

// End EnkiChangeEventThread.java
