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

import javax.jmi.reflect.*;

import org.netbeans.api.mdr.events.*;

/**
 * CreateInstanceEventValidator extends {@link InstanceEvent} to allow setting
 * the instance field after the object is created.  This allows planned-change
 * events to be fired with a null instance (as required by the MDR Events API)
 * and then to change the event object to return the actual instance for
 * the canceled-change or change events at rollback/commit time.  Note that
 * the MDR Events API requires that the same event object instance be
 * delivered at both times.
 * 
 * @author Stephan Zuercher
 */
public class CreateInstanceEvent extends InstanceEvent
{
    private static final long serialVersionUID = 6919823214252246999L;

    public CreateInstanceEvent(RefFeatured source, List<Object> arguments)
    {
        super(source, InstanceEvent.EVENT_INSTANCE_CREATE, arguments, null);
    }
    
    public void setInstance(RefObject instance)
    {
        assert(getInstance() == null);
        
        this.instance = instance;
    }
}

// End CreateInstanceEventValidator.java
