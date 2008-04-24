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
    private static final long POLLING_TIMEOUT = 1000L;
    private static final int MAX_CONSECUTIVE = 100;
    private static final int MAX_CAPACITY = 1000;
    
    private static final Logger log = 
        Logger.getLogger(EnkiChangeEventThread.class.getName());
    
    private final ListenerSource listenerSource;
    private final LinkedBlockingQueue<MDRChangeEvent> eventQueue;
    
    private boolean shutdown;
    
    public EnkiChangeEventThread(ListenerSource listenerSource)
    {
        this.listenerSource = listenerSource;
        this.eventQueue = 
            new LinkedBlockingQueue<MDRChangeEvent>(MAX_CAPACITY);
        this.shutdown = false;
        
        setName("Enki MDR Change Event Thread");
        setDaemon(true);
    }
    
    public synchronized void shutdown() throws InterruptedException
    {
        if (shutdown) {
            return;
        }

        eventQueue.clear();
        shutdown = true;
        
        join();
    }
    
    public void enqueueEvent(MDRChangeEvent event)
    {
        eventQueue.offer(event);
    }
    
    public void run()
    {
        ArrayList<MDRChangeEvent> events = 
            new ArrayList<MDRChangeEvent>(MAX_CONSECUTIVE + 1);

        boolean sawEvents = false;
        try {
            while(true) {
                synchronized(this) {
                    if (shutdown) {
                        break;
                    }
                }

                if (!sawEvents) {
                    MDRChangeEvent trigger = 
                       eventQueue.poll(
                           POLLING_TIMEOUT, TimeUnit.MILLISECONDS);

                    if (trigger == null) {
                        sawEvents = false;
                        continue;
                    }
                    
                    events.add(trigger);
                } else {
                    int numAdded = eventQueue.drainTo(events, MAX_CONSECUTIVE);
                    
                    if (numAdded == 0) {
                        sawEvents = false;
                        continue;
                    }
                }

                sawEvents = true;
                    
                Collection<EnkiMaskedMDRChangeListener> listeners= 
                    listenerSource.getListeners();
                for(MDRChangeEvent event: events) {                        
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
                }

                events.clear();
            }
        }
        catch(InterruptedException e) {
            log.log(Level.SEVERE, "EnkiChangeEventThread interrupted", e);
        }
        catch(Throwable t) {
            log.log(
                Level.SEVERE, 
                "EnkiChangeEventThread ending unexpectedly",
                t);
        }
    }
    
    /**
     * ListenerSource represents an object that is a source of 
     * {@link MDRChangeListener} instances.  Typically, this object is
     * also an {@link EnkiMDRepository}.
     */
    public interface ListenerSource
    {
        /**
         * Return all current listeners .
         * 
         * @return listeners collection that will contain all current listeners
         */
        public Collection<EnkiMaskedMDRChangeListener> getListeners();
    }
}

// End EnkiChangeEventThread.java
