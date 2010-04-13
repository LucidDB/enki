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

import org.junit.*;
import org.netbeans.api.mdr.events.*;

/**
 * DelegatingEventValidator delegates validation, by sequence number, to a
 * collection of {@link EventValidator} instances.  Each EventValidator is
 * configured with a reference to the DelegatingEventValidator instance so that
 * they can communicate, if necessary.
 * 
 * @author Stephan Zuercher
 */
public class DelegatingEventValidator extends EventValidator
{
    private final EventValidator[] validators;
    
    public DelegatingEventValidator(EventValidator... validatorsInit)
    {
        super(null);
        
        this.validators = validatorsInit;
        
        for(int i = 0; i < validators.length; i++) {
            if (validators[i] instanceof DuplicateEventValidator) {
                validators[i] = 
                    ((DuplicateEventValidator)validators[i]).makeClone(this);
            }
            validators[i].setDelegatingEventValidator(this);
            
        }
    }
    
    @Override
    public int getNumExpectedEvents()
    {
        int n = 0;
        for(EventValidator validator: validators) {
            n += validator.getNumExpectedEvents();
        }
        return n;
    }
    
    @Override
    public void rethrow()
    {
        super.rethrow();
        
        for(EventValidator validator: validators) {
            validator.rethrow();
        }
    }
    
    public EventValidator getValidator(int seq)
    {
        return validators[seq];
    }
    
    @Override
    public void visitEvent(
        MDRChangeEvent event, EventType eventType, int seq)
    {
        try {
            if (seq < 0) {
                Assert.fail("invalid seq#: " + seq);
            } else if (seq >= validators.length) {
                Assert.fail("too many events at seq#" + seq);
            }
            
            validators[seq].visitEvent(event, eventType, seq);
        }
        catch(Throwable t) {
            if (thrown == null) {
                thrown = t;
            }
        }
    }

    @Override
    EventValidator cloneWithNewExpectedEventType(EventType newEventType)
    {
        throw new UnsupportedOperationException();
    }
}

// End DelegatingEventValidator.java
