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
 * EnkiMaskedMDRChangeListener wraps {@link MDRPreChangeListener} and 
 * automatically propagates or blocks pre-change and change events according
 * to the specified event mask.  In addition, pre-change events are 
 * automatically discarded if the given listener is not an 
 * {@link MDRPreChangeListener}.  Events are also discarded if the
 * listener throws any {@link RuntimeException}.
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
    
    public void changeCancelled(MDRChangeEvent event)
    {
        if (preListener == null) {
            return;
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

    public void plannedChange(MDRChangeEvent event)
    {
        if (preListener == null) {
            return;
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

    public void change(MDRChangeEvent event)
    {
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
    
    public void add(int addMask)
    {
        mask = mask | addMask;
    }
    
    public boolean remove(int removeMask)
    {
        mask = mask & ~removeMask;
        
        return mask != 0;
    }
    
    @Override
    public boolean equals(Object o)
    {
        EnkiMaskedMDRChangeListener that = (EnkiMaskedMDRChangeListener)o;
        
        return this.listener == that.listener;
    }
    
    @Override
    public int hashCode()
    {
        return listener.hashCode();
    }
}

// End EnkiMaskedMDRChangeListener.java
