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

import java.util.logging.*;

import org.netbeans.api.mdr.events.*;

/**
 * EnkiMaskedMDRChangeListener wraps an {@link MDRChangeListener} and 
 * automatically propagates events according to the specified event mask.
 * If the given listener is also an {@link MDRPreChangeListener}, pre-change
 * events are also propagated, other they are discarded. Events are also 
 * discarded if the listener throws any {@link RuntimeException}.
 * 
 * <p><b>Mask Bits</b></p>
 * The mask bits used by this class correspond to the constants fields in
 * {@link MDRChangeEvent} and its subclasses in the MDR API.  Mask bits
 * may be combined to produce the desired set of events for a listener.
 * 
 * @author Stephan Zuercher
 */
public class EnkiMaskedMDRChangeListener implements MDRPreChangeListener
{
    private static final Logger log = 
        Logger.getLogger(EnkiMaskedMDRChangeListener.class.getName());
    
    private final MDRChangeListener listener;
    private final MDRPreChangeListener preListener;
    private int mask;
    
    /**
     * Constructs an EnkiMaskedMDRChangeListener with the given mask.
     * See class description for explanation of mask bits.
     * 
     * @param listener {@link MDRChangeListener} (or 
     *                 {@link MDRPreChangeListener}) to invoke for events
     *                 matching the mask
     * @param mask mask bits for the desired events
     */
    public EnkiMaskedMDRChangeListener(MDRChangeListener listener, int mask)
    {
        this.listener = listener;
        if (listener instanceof MDRPreChangeListener) {
            this.preListener = (MDRPreChangeListener)listener;
        } else {
            this.preListener = null;
        }
        this.mask = mask;
    }
    
    // Implements MDRPreChangeListener
    public void changeCancelled(MDRChangeEvent event)
    {
        if (preListener == null) {
            return;
        }

        int mask;
        synchronized(this) {
            mask = this.mask;
        }
               
        if (event.isOfType(mask)) {
            try {
                preListener.changeCancelled(event);
            }
            catch(RuntimeException e) {
                log.log(
                    Level.WARNING, 
                    "Ignored exception during changeCancelled event", 
                    e);
            }
        }
    }

    // Implements MDRPreChangeListener
    public void plannedChange(MDRChangeEvent event)
    {
        if (preListener == null) {
            return;
        }
        
        int mask;
        synchronized(this) {
            mask = this.mask;
        }
        
        if (event.isOfType(mask)) {
            try {
                preListener.plannedChange(event);
            }
            catch(RuntimeException e) {
                log.log(
                    Level.WARNING, 
                    "Ignored exception during plannedChange event", 
                    e);
            }
        }
    }

    // Implements MDRChangeListener
    public void change(MDRChangeEvent event)
    {
        int mask;
        synchronized(this) {
            mask = this.mask;
        }
        
        if (event.isOfType(mask)) {
            try {
                listener.change(event);
            }
            catch(RuntimeException e) {
                log.log(
                    Level.WARNING, 
                    "Ignored exception during change event", 
                    e);
            }
        }
    }
    
    /**
     * Updates this instance's mask to include the given mask bits.  Performs 
     * a bitwise OR of the current mask with the new mask.  Mask bits are 
     * described in the class description.
     * 
     * @param addMask additional mask bits to set
     * @see EnkiMaskedMDRChangeListener
     */
    public synchronized void add(int addMask)
    {
        mask = mask | addMask;
    }
    
    /**
     * Updates this instance's mask to exclude the given mask bits.  Performs
     * a bitwise negation of the <code>removeMask</code> and then a bitwise
     * AND of the result with the current mask bits. Mask bits are described 
     * in the class description.
     * 
     * @param removeMask mask bits to clear
     * @see EnkiMaskedMDRChangeListener
     */
    public synchronized boolean remove(int removeMask)
    {
        mask = mask & ~removeMask;
        
        return mask != 0;
    }
    
    /**
     * Compares two EnkiMaskedMDRChangeListener instances for equality by
     * identity of the underlying {@link MDRChangeListener}.
     */
    @Override
    public boolean equals(Object o)
    {
        EnkiMaskedMDRChangeListener that = (EnkiMaskedMDRChangeListener)o;
        
        return this.listener == that.listener;
    }
    
    /**
     * Delegates to the {@link MDRChangeListener}'s {@link Object#hashCode()}
     * method. 
     */
    @Override
    public int hashCode()
    {
        return listener.hashCode();
    }
}

// End EnkiMaskedMDRChangeListener.java
